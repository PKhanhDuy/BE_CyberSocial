import json
import logging
import re
import urllib.error
import urllib.request

from app.schemas import EventAttribution
from app.settings import settings

logger = logging.getLogger(__name__)

_SYSTEM_PROMPT = """\
Bạn là trợ lý viết giải thích kết quả phát hiện tin giả cho người dùng phổ thông.
Chỉ dùng thông tin trong JSON đầu vào.
Không thêm sự kiện, người dùng, hoặc số liệu mới.
Không đổi nhãn FAKE/REAL.
Không nhắc TGNN, TIGE, mô hình, hay thuật ngữ kỹ thuật.
Viết tiếng Việt tự nhiên, dễ hiểu.
Trả về JSON duy nhất với đúng 2 khóa: headline, narrative.
headline: 1 câu ngắn (tối đa 120 ký tự).
narrative: 2-4 câu, có mạch thời gian nếu có sự kiện quan trọng.
"""


def build_narration_payload(
    label: str,
    fake_probability: float,
    graph_event_count: int,
    context_hints: list[str],
    attributions: list[EventAttribution],
    headline: str,
    narrative: str,
) -> dict:
    key_events = []
    for item in attributions[:3]:
        key_events.append(
            {
                "actor": item.actorLabel or "một người dùng",
                "action": item.eventTypeLabel or item.eventType,
                "time": item.relativeTime,
                "impactLevel": item.impactLevel,
                "summary": item.summary,
            }
        )

    return {
        "label": label,
        "fakeProbabilityPercent": round(fake_probability * 100, 1),
        "graphEventCount": graph_event_count,
        "contextHints": context_hints,
        "keyEvents": key_events,
        "templateHeadline": headline,
        "templateNarrative": narrative,
    }


def _extract_json_object(text: str) -> dict | None:
    text = text.strip()
    if not text:
        return None

    try:
        parsed = json.loads(text)
        return parsed if isinstance(parsed, dict) else None
    except json.JSONDecodeError:
        pass

    fenced = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", text, flags=re.DOTALL)
    if fenced:
        try:
            parsed = json.loads(fenced.group(1))
            return parsed if isinstance(parsed, dict) else None
        except json.JSONDecodeError:
            return None

    start = text.find("{")
    end = text.rfind("}")
    if start >= 0 and end > start:
        try:
            parsed = json.loads(text[start : end + 1])
            return parsed if isinstance(parsed, dict) else None
        except json.JSONDecodeError:
            return None
    return None


def _validate_narration(payload: dict, headline: str, narrative: str) -> bool:
    if not headline or not narrative:
        return False
    if len(headline) > 180 or len(narrative) > 1200:
        return False

    allowed_actors = {
        str(event.get("actor", "")).strip().lower()
        for event in payload.get("keyEvents", [])
        if str(event.get("actor", "")).strip()
    }
    if not allowed_actors:
        return True

    narrative_lower = narrative.lower()
    for actor in allowed_actors:
        if actor and actor not in {"một người dùng", "mot nguoi dung"} and actor in narrative_lower:
            return True
    return len(narrative.split()) >= 8


def _build_user_prompt(payload: dict) -> str:
    return (
        "Viết lại headline và narrative dựa trên JSON sau.\n"
        "Giữ nguyên ý nghĩa, chỉ làm câu chữ tự nhiên hơn.\n\n"
        f"{json.dumps(payload, ensure_ascii=False)}"
    )


def _parse_llm_response(raw: str | None, provider: str, payload: dict, headline: str, narrative: str) -> tuple[str, str]:
    if not raw:
        return headline, narrative

    parsed = _extract_json_object(raw)
    if not parsed:
        logger.warning("%s returned non-JSON narration", provider)
        return headline, narrative

    new_headline = str(parsed.get("headline", "")).strip()
    new_narrative = str(parsed.get("narrative", "")).strip()
    if not _validate_narration(payload, new_headline, new_narrative):
        logger.warning("%s narration rejected by validator", provider)
        return headline, narrative

    return new_headline, new_narrative


def _call_ollama(prompt: str) -> str | None:
    body = json.dumps(
        {
            "model": settings.ollama_model,
            "messages": [
                {"role": "system", "content": _SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            "stream": False,
            "format": "json",
        }
    ).encode("utf-8")

    request = urllib.request.Request(
        f"{settings.ollama_base_url.rstrip('/')}/api/chat",
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=settings.ollama_timeout_seconds) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
        logger.warning("Ollama narration failed: %s", exc)
        return None

    message = payload.get("message") or {}
    content = message.get("content")
    return content if isinstance(content, str) else None


def _call_gemini(prompt: str) -> str | None:
    if not settings.gemini_api_key:
        logger.warning("Gemini narration enabled but GEMINI_API_KEY is missing")
        return None

    url = (
        "https://generativelanguage.googleapis.com/v1beta/models/"
        f"{settings.gemini_model}:generateContent?key={settings.gemini_api_key}"
    )
    body = json.dumps(
        {
            "systemInstruction": {"parts": [{"text": _SYSTEM_PROMPT}]},
            "contents": [{"role": "user", "parts": [{"text": prompt}]}],
            "generationConfig": {
                "responseMimeType": "application/json",
                "temperature": 0.4,
            },
        }
    ).encode("utf-8")

    request = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=settings.gemini_timeout_seconds) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
        logger.warning("Gemini narration failed: %s", exc)
        return None

    candidates = payload.get("candidates") or []
    if not candidates:
        logger.warning("Gemini narration returned no candidates")
        return None

    parts = (candidates[0].get("content") or {}).get("parts") or []
    if not parts:
        return None

    text = parts[0].get("text")
    return text if isinstance(text, str) else None


def maybe_enhance_with_llm(
    headline: str,
    narrative: str,
    label: str,
    fake_probability: float,
    graph_event_count: int,
    context_hints: list[str],
    attributions: list[EventAttribution],
) -> tuple[str, str]:
    provider = settings.llm_narration_provider
    if provider == "none":
        return headline, narrative

    payload = build_narration_payload(
        label=label,
        fake_probability=fake_probability,
        graph_event_count=graph_event_count,
        context_hints=context_hints,
        attributions=attributions,
        headline=headline,
        narrative=narrative,
    )
    prompt = _build_user_prompt(payload)

    if provider == "gemini":
        raw = _call_gemini(prompt)
        return _parse_llm_response(raw, "Gemini", payload, headline, narrative)

    if provider == "ollama":
        raw = _call_ollama(prompt)
        return _parse_llm_response(raw, "Ollama", payload, headline, narrative)

    logger.warning("Unknown LLM_NARRATION_PROVIDER=%s, skipping narration", provider)
    return headline, narrative


def maybe_enhance_with_ollama(
    headline: str,
    narrative: str,
    label: str,
    fake_probability: float,
    graph_event_count: int,
    context_hints: list[str],
    attributions: list[EventAttribution],
) -> tuple[str, str]:
    return maybe_enhance_with_llm(
        headline=headline,
        narrative=narrative,
        label=label,
        fake_probability=fake_probability,
        graph_event_count=graph_event_count,
        context_hints=context_hints,
        attributions=attributions,
    )
