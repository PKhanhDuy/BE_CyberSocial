from app.schemas import AnalyzeRequest
from app.settings import settings


def resolve_inference_mode(request: AnalyzeRequest) -> str:
    if not request.events:
        if not settings.allow_text_only:
            raise ValueError(
                "Propagation events are required. Set TGNN_ALLOW_TEXT_ONLY=true to allow degraded text-only mode."
            )
        return "text_only_fallback"
    return "full"


def validate_analyze_request(request: AnalyzeRequest) -> None:
    text = request.text.strip()
    if not text:
        raise ValueError("text must not be empty")

    if len(request.events) > settings.max_events_per_article:
        raise ValueError(
            f"events exceeds TGNN_MAX_EVENTS limit ({settings.max_events_per_article})"
        )

    resolve_inference_mode(request)
