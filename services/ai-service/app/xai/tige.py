from dataclasses import dataclass, field

import torch
import torch.nn.functional as F

from app.model.tgnn import TGNN
from app.schemas import InteractionEvent


@dataclass
class TigeEventScore:
    event_index: int
    event_type: str
    tige_removal: float
    confidence_drop: float
    conditional_tige: float | None = None


@dataclass
class TigeResult:
    target_class: int
    fake_probability: float
    full_entropy: float
    scores: list[TigeEventScore] = field(default_factory=list)


def predictive_entropy(probs: torch.Tensor, eps: float = 1e-12) -> torch.Tensor:
    clamped = torch.clamp(probs, min=eps, max=1.0)
    return -torch.sum(clamped * torch.log(clamped), dim=-1)


def predict_probs(
    model: TGNN,
    text_h: torch.Tensor,
    user_idx: torch.Tensor,
    edge_feats: torch.Tensor,
    temperature: float,
) -> torch.Tensor:
    model.eval()
    with torch.no_grad():
        logits, _ = model.forward_logits(text_h, user_idx, edge_feats)
        return F.softmax(logits.float() / temperature, dim=-1)


def _mask_event_rows(edge_feats: torch.Tensor, event_indices: list[int]) -> torch.Tensor:
    masked = edge_feats.clone()
    if event_indices:
        masked[event_indices] = 0.0
    return masked


def _prefix_tensors(
    user_idx: torch.Tensor,
    edge_feats: torch.Tensor,
    end_exclusive: int,
) -> tuple[torch.Tensor, torch.Tensor]:
    if end_exclusive <= 0:
        empty_users = torch.zeros(1, dtype=torch.long, device=edge_feats.device)
        empty_edges = torch.zeros((1, edge_feats.shape[1]), dtype=edge_feats.dtype, device=edge_feats.device)
        return empty_users, empty_edges
    return user_idx[:end_exclusive], edge_feats[:end_exclusive]


def compute_tige(
    model: TGNN,
    text_h: torch.Tensor,
    user_idx: torch.Tensor,
    edge_feats: torch.Tensor,
    events: list[InteractionEvent],
    temperature: float,
) -> TigeResult | None:
    event_count = edge_feats.shape[0]
    if event_count <= 1:
        return None

    full_probs = predict_probs(model, text_h, user_idx, edge_feats, temperature)
    target_class = int(torch.argmax(full_probs, dim=-1).item())
    full_target_prob = float(full_probs[0, target_class].item())
    full_entropy = float(predictive_entropy(full_probs)[0].item())
    fake_probability = float(full_probs[0, 1].item())

    removal_scores: dict[int, tuple[float, float]] = {}
    for event_idx in range(event_count):
        masked_edges = _mask_event_rows(edge_feats, [event_idx])
        removed_probs = predict_probs(model, text_h, user_idx, masked_edges, temperature)
        removed_entropy = float(predictive_entropy(removed_probs)[0].item())
        removed_target_prob = float(removed_probs[0, target_class].item())
        removal_scores[event_idx] = (
            removed_entropy - full_entropy,
            full_target_prob - removed_target_prob,
        )

    conditional_scores: dict[int, float] = {}
    prev_users, prev_edges = _prefix_tensors(user_idx, edge_feats, 0)
    prev_probs = predict_probs(model, text_h, prev_users, prev_edges, temperature)
    prev_entropy = float(predictive_entropy(prev_probs)[0].item())

    for event_idx in range(event_count):
        curr_users, curr_edges = _prefix_tensors(user_idx, edge_feats, event_idx + 1)
        curr_probs = predict_probs(model, text_h, curr_users, curr_edges, temperature)
        curr_entropy = float(predictive_entropy(curr_probs)[0].item())
        conditional_scores[event_idx] = prev_entropy - curr_entropy
        prev_entropy = curr_entropy

    scores: list[TigeEventScore] = []
    for event_idx in range(event_count):
        event_type = events[event_idx].eventType if event_idx < len(events) else "event"
        removal_gain, confidence_drop = removal_scores[event_idx]
        scores.append(
            TigeEventScore(
                event_index=event_idx,
                event_type=event_type,
                tige_removal=removal_gain,
                confidence_drop=confidence_drop,
                conditional_tige=conditional_scores.get(event_idx),
            )
        )

    return TigeResult(
        target_class=target_class,
        fake_probability=fake_probability,
        full_entropy=full_entropy,
        scores=scores,
    )
