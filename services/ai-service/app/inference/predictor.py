import torch
import torch.nn.functional as F

from app.inference.features import build_edge_matrix
from app.inference.loader import LoadedArtifact, load_artifact
from app.inference.users import build_user_index_tensor
from app.inference.validation import resolve_inference_mode, validate_analyze_request
from app.schemas import AnalyzeRequest, AnalyzeResponse, InteractionEvent
from app.settings import settings
from app.xai.explanation import (
    build_basic_explanation,
    build_event_attributions,
    build_explanation_content,
    build_propagation_timeline,
)
from app.xai.tige import compute_tige


class ModelNotLoadedError(RuntimeError):
    pass


def _events_for_inference(request: AnalyzeRequest) -> list[InteractionEvent]:
    if request.events:
        return sorted(
            request.events,
            key=lambda event: event.timestampUnix if event.timestampUnix is not None else 0.0,
        )[: settings.max_events_per_article]

    return [
        InteractionEvent(
            dstArticleId=request.articleId,
            textLen=len(request.text),
            eventType="tweet",
        )
    ]


class TGNNPredictor:
    def __init__(self, eager_load: bool = True) -> None:
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.artifact: LoadedArtifact | None = None
        self.load_error: str | None = None
        if eager_load:
            self.load()

    @property
    def is_loaded(self) -> bool:
        return self.artifact is not None

    def load(self) -> None:
        try:
            self.artifact = load_artifact(settings.model_path, device=self.device)
            self.load_error = None
        except Exception as exc:  # noqa: BLE001
            self.artifact = None
            self.load_error = str(exc)

    def encode_text(self, text: str) -> torch.Tensor:
        if self.artifact is None:
            raise ModelNotLoadedError(self.load_error or "TGNN model is not loaded")

        with torch.no_grad():
            embedding = self.artifact.sentence_transformer.encode(
                text,
                normalize_embeddings=True,
                convert_to_tensor=True,
                show_progress_bar=False,
            )
            if embedding.dim() == 1:
                embedding = embedding.unsqueeze(0)
            return embedding.clone().to(self.device)

    def _prepare_tensors(
        self,
        request: AnalyzeRequest,
    ) -> tuple[torch.Tensor, torch.Tensor, torch.Tensor, list[InteractionEvent], int]:
        if self.artifact is None:
            raise ModelNotLoadedError(self.load_error or "TGNN model is not loaded")

        edge_matrix = build_edge_matrix(
            request,
            edge_mean=self.artifact.edge_mean,
            edge_std=self.artifact.edge_std,
            event_type2idx=self.artifact.event_type2idx or None,
        )
        edge_feats = torch.tensor(edge_matrix, dtype=torch.float32, device=self.device)
        feature_events = _events_for_inference(request)
        user_idx = build_user_index_tensor(
            feature_events,
            user2idx=self.artifact.user2idx,
            device=self.device,
        )
        if user_idx.size(0) != edge_feats.size(0):
            user_idx = torch.zeros(edge_feats.size(0), dtype=torch.long, device=self.device)

        with torch.no_grad():
            text_embedding = self.encode_text(request.text)
            text_h = self.artifact.model.text_encoder(text_embedding)

        return text_h, user_idx, edge_feats, feature_events, edge_feats.size(0)

    def predict(self, request: AnalyzeRequest) -> tuple[torch.Tensor, torch.Tensor, int]:
        text_h, user_idx, edge_feats, _, graph_event_count = self._prepare_tensors(request)
        with torch.no_grad():
            logits, gate = self.artifact.model.forward_logits(text_h, user_idx, edge_feats)
        return logits, gate, graph_event_count

    def analyze(self, request: AnalyzeRequest) -> AnalyzeResponse:
        validate_analyze_request(request)
        mode = resolve_inference_mode(request)
        text_h, user_idx, edge_feats, feature_events, graph_event_count = self._prepare_tensors(request)

        with torch.no_grad():
            logits, gate = self.artifact.model.forward_logits(text_h, user_idx, edge_feats)

        probabilities = F.softmax(logits.float(), dim=-1)[0]
        fake_probability = float(probabilities[1].detach().cpu())
        threshold = self.artifact.threshold if self.artifact else settings.threshold
        label = "FAKE" if fake_probability >= threshold else "REAL"
        risk_level = self._risk_level(fake_probability)
        event_attributions = []
        propagation_timeline = []
        tige_result = None
        target_class = int(torch.argmax(probabilities).item())
        headline = None
        narrative = None
        context_hints: list[str] = []
        explanation = ""

        if mode == "text_only_fallback":
            headline, narrative, explanation, context_hints = build_explanation_content(
                label,
                fake_probability,
                threshold,
                graph_event_count,
                feature_events,
                [],
                mode=mode,
            )
        elif request.includeXai and graph_event_count > 1:
            tige_user_idx = user_idx
            tige_edge_feats = edge_feats
            tige_events = feature_events
            if graph_event_count > settings.tige_max_events:
                tige_user_idx = user_idx[: settings.tige_max_events]
                tige_edge_feats = edge_feats[: settings.tige_max_events]
                tige_events = feature_events[: settings.tige_max_events]

            tige_result = compute_tige(
                self.artifact.model,
                text_h,
                tige_user_idx,
                tige_edge_feats,
                tige_events,
                self.artifact.tige_temperature,
            )
            if tige_result is not None:
                target_class = tige_result.target_class
                event_attributions = build_event_attributions(
                    tige_result,
                    feature_events,
                    settings.tige_top_k,
                )
                propagation_timeline = build_propagation_timeline(
                    feature_events,
                    tige_result,
                    settings.tige_top_k,
                )
                headline, narrative, explanation, context_hints = build_explanation_content(
                    label,
                    fake_probability,
                    threshold,
                    graph_event_count,
                    feature_events,
                    event_attributions,
                    mode=mode,
                )
            else:
                headline, narrative, explanation, context_hints = build_explanation_content(
                    label,
                    fake_probability,
                    threshold,
                    graph_event_count,
                    feature_events,
                    [],
                    mode=mode,
                )
        else:
            headline, narrative, explanation, context_hints = build_explanation_content(
                label,
                fake_probability,
                threshold,
                graph_event_count,
                feature_events,
                [],
                mode=mode,
            )

        return AnalyzeResponse(
            fakeProbability=fake_probability,
            explanation=explanation,
            headline=headline,
            narrative=narrative,
            contextHints=context_hints,
            riskLevel=risk_level,
            label=label,
            threshold=threshold,
            eventCount=len(request.events),
            graphContribution=float(gate.mean().detach().cpu()),
            mode=mode,
            targetClass=target_class,
            eventAttributions=event_attributions,
            propagationTimeline=propagation_timeline,
        )

    @staticmethod
    def _risk_level(fake_probability: float) -> str:
        if fake_probability >= 0.75:
            return "HIGH"
        if fake_probability >= 0.45:
            return "MEDIUM"
        return "LOW"
