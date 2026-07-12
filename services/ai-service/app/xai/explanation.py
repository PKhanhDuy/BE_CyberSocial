from app.schemas import EventAttribution, InteractionEvent, PropagationTimelineEvent
from app.xai.tige import TigeEventScore, TigeResult

_EVENT_TYPE_LABELS = {
    "tweet": "Đăng bài",
    "retweet": "Chia sẻ",
    "reply": "Trả lời",
    "quote": "Trích dẫn",
    "like": "Thích",
    "comment": "Bình luận",
    "share": "Chia sẻ",
}


def _event_type_label(event_type: str) -> str:
    return _EVENT_TYPE_LABELS.get(event_type, event_type)


def _actor_label(event: InteractionEvent) -> str:
    if event.actorDisplayName and event.actorDisplayName.strip():
        return event.actorDisplayName.strip()
    return "Người dùng"


def _format_relative_delta(events: list[InteractionEvent], event_index: int) -> str:
    if not events or event_index >= len(events):
        return "0"

    anchor = next(
        (event.timestampUnix for event in events if event.timestampUnix is not None),
        None,
    )
    current = events[event_index].timestampUnix
    if anchor is None or current is None:
        order = events[event_index].eventOrder
        if order is not None:
            return f"+{int(order)}"
        return f"+{event_index}"

    delta_seconds = max(0, int(current - anchor))
    if delta_seconds < 60:
        return f"+{delta_seconds}s"
    if delta_seconds < 3600:
        return f"+{delta_seconds // 60}m"
    return f"+{delta_seconds // 3600}h"


def _format_timeline_label(events: list[InteractionEvent], event_index: int) -> str:
    if event_index == 0:
        return "t=0"
    return f"t={_format_relative_delta(events, event_index)}"


def _human_event_label(event: InteractionEvent, relative_time: str) -> str:
    action = _event_type_label(event.eventType)
    actor = _actor_label(event)
    if event.eventType == "tweet":
        return f"{action} bởi {actor} ({relative_time})"
    return f"{action} bởi {actor} ({relative_time})"


def _event_summary(
    event: InteractionEvent,
    relative_time: str,
    score: TigeEventScore,
) -> str:
    action = _event_type_label(score.event_type)
    actor = _actor_label(event)
    direction = "tăng" if score.confidence_drop > 0 else "giảm"
    return (
        f"{action} bởi {actor} lúc {relative_time} {direction} xác suất tin giả "
        f"(TIGE {score.tige_removal:+.3f})."
    )


def build_event_attributions(
    tige_result: TigeResult,
    events: list[InteractionEvent],
    top_k: int,
) -> list[EventAttribution]:
    ranked = sorted(
        tige_result.scores,
        key=lambda score: abs(score.tige_removal),
        reverse=True,
    )[:top_k]

    attributions: list[EventAttribution] = []
    for score in ranked:
        event = events[score.event_index] if score.event_index < len(events) else None
        relative_time = _format_timeline_label(events, score.event_index)
        actor_label = _actor_label(event) if event is not None else "Người dùng"
        attributions.append(
            EventAttribution(
                eventIndex=score.event_index,
                eventType=score.event_type,
                eventTypeLabel=_event_type_label(score.event_type),
                relativeTime=relative_time,
                actorLabel=actor_label,
                tigeRemoval=round(score.tige_removal, 6),
                confidenceDrop=round(score.confidence_drop, 6),
                summary=_event_summary(
                    event if event is not None else InteractionEvent(eventType=score.event_type),
                    relative_time,
                    score,
                ),
            )
        )
    return attributions


def build_propagation_timeline(
    events: list[InteractionEvent],
    tige_result: TigeResult | None,
    top_k: int,
) -> list[PropagationTimelineEvent]:
    if not events:
        return []

    influential_indices: set[int] = set()
    score_by_index: dict[int, TigeEventScore] = {}
    if tige_result is not None:
        ranked = sorted(
            tige_result.scores,
            key=lambda score: abs(score.tige_removal),
            reverse=True,
        )[:top_k]
        influential_indices = {score.event_index for score in ranked}
        score_by_index = {score.event_index: score for score in tige_result.scores}

    timeline: list[PropagationTimelineEvent] = []
    for index, event in enumerate(events):
        relative_time = _format_timeline_label(events, index)
        score = score_by_index.get(index)
        depth = 0
        if event.tree is not None and event.tree.depth is not None:
            depth = int(event.tree.depth)
        timeline.append(
            PropagationTimelineEvent(
                eventIndex=index,
                eventId=event.eventId,
                parentEventId=event.parentEventId,
                depth=depth,
                relativeTime=relative_time,
                eventType=event.eventType,
                eventTypeLabel=_event_type_label(event.eventType),
                actorLabel=_actor_label(event),
                tigeRemoval=round(score.tige_removal, 6) if score is not None else None,
                isInfluential=index in influential_indices,
            )
        )
    return timeline


def build_tige_explanation(
    label: str,
    fake_probability: float,
    threshold: float,
    graph_event_count: int,
    attributions: list[EventAttribution],
) -> str:
    verdict = "tin giả" if label == "FAKE" else "tin thật"
    base = (
        f"Mô hình TGNN dự đoán {verdict} với xác suất {fake_probability * 100:.1f}% "
        f"(ngưỡng {(threshold * 100):.0f}%) dựa trên {graph_event_count} sự kiện lan truyền."
    )
    if not attributions:
        return base + " TIGE không tìm thấy sự kiện lan truyền nổi bật."

    signal_parts = []
    for item in attributions[:3]:
        action = item.eventTypeLabel or _event_type_label(item.eventType)
        actor = item.actorLabel or "Người dùng"
        time_label = item.relativeTime or "t=?"
        signal_parts.append(f"{action} bởi {actor} lúc {time_label}")
    signals = "; ".join(signal_parts)
    return (
        f"{base} Các tín hiệu lan truyền chính: {signals}. "
        f"Sự kiện ảnh hưởng nhất: {attributions[0].summary}"
    )
