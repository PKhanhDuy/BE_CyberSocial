from app.schemas import EventAttribution, InteractionEvent, PropagationTimelineEvent
from app.xai.tige import TigeEventScore, TigeResult

_EVENT_TYPE_LABELS = {
    "tweet": "Đăng bài",
    "retweet": "Đăng lại",
    "reply": "Trả lời",
    "quote": "Trích dẫn",
    "like": "Thích",
    "comment": "Bình luận",
    "share": "Chia sẻ",
}

_ACTION_VERBS = {
    "tweet": "đăng bài",
    "retweet": "đăng lại",
    "reply": "trả lời",
    "quote": "trích dẫn",
    "like": "thích bài",
    "comment": "bình luận",
    "share": "chia sẻ",
}


def _event_type_label(event_type: str) -> str:
    return _EVENT_TYPE_LABELS.get(event_type, event_type)


def _action_verb(event_type: str) -> str:
    return _ACTION_VERBS.get(event_type, _event_type_label(event_type).lower())


def _actor_label(event: InteractionEvent) -> str:
    if event.actorDisplayName and event.actorDisplayName.strip():
        return event.actorDisplayName.strip()
    return "một người dùng"


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


def _natural_time_from_label(relative_time: str) -> str:
    if relative_time in {"t=0", "0"}:
        return "ngay khi đăng bài"

    normalized = relative_time.removeprefix("t=").removeprefix("+")
    if normalized.isdigit():
        seconds = int(normalized)
    elif normalized.endswith("s") and normalized[:-1].isdigit():
        seconds = int(normalized[:-1])
    elif normalized.endswith("m") and normalized[:-1].isdigit():
        minutes = int(normalized[:-1])
        if minutes == 1:
            return "sau 1 phút"
        return f"sau {minutes} phút"
    elif normalized.endswith("h") and normalized[:-1].isdigit():
        hours = int(normalized[:-1])
        if hours == 1:
            return "sau 1 giờ"
        return f"sau {hours} giờ"
    else:
        return f"sau {relative_time.removeprefix('t=')}"

    if seconds < 60:
        if seconds <= 1:
            return "sau vài giây"
        return f"sau {seconds} giây"
    if seconds < 3600:
        minutes = seconds // 60
        if minutes == 1:
            return "sau 1 phút"
        return f"sau {minutes} phút"
    hours = seconds // 3600
    if hours == 1:
        return "sau 1 giờ"
    return f"sau {hours} giờ"


def _impact_level(tige_removal: float) -> str:
    magnitude = abs(tige_removal)
    if magnitude >= 0.06:
        return "mạnh"
    if magnitude >= 0.03:
        return "vừa"
    return "nhẹ"


def _impact_level_code(tige_removal: float) -> str:
    magnitude = abs(tige_removal)
    if magnitude >= 0.06:
        return "high"
    if magnitude >= 0.03:
        return "medium"
    return "low"


def build_headline(label: str, fake_probability: float) -> str:
    pct = fake_probability * 100
    if label == "FAKE":
        if pct >= 80:
            return "Bài này có khả năng cao là tin giả"
        if pct >= 60:
            return "Bài này có dấu hiệu tin giả"
        return "Bài này có thể là tin giả"
    if pct <= 20:
        return "Bài này có vẻ đáng tin"
    if pct <= 40:
        return "Bài này nhiều khả năng đáng tin"
    return "Bài này có thể đáng tin"


def _event_delta_seconds(events: list[InteractionEvent], event_index: int) -> int | None:
    if not events or event_index <= 0 or event_index >= len(events):
        return None
    anchor = next(
        (event.timestampUnix for event in events if event.timestampUnix is not None),
        None,
    )
    current = events[event_index].timestampUnix
    if anchor is None or current is None:
        return None
    return max(0, int(current - anchor))


def detect_context_hints(events: list[InteractionEvent], label: str) -> list[str]:
    if len(events) <= 1:
        return []

    hints: list[str] = []
    non_root = list(enumerate(events))[1:]
    share_count = sum(
        1 for _, event in non_root if event.eventType in {"share", "retweet", "quote"}
    )
    like_count = sum(1 for _, event in non_root if event.eventType == "like")
    comment_count = sum(1 for _, event in non_root if event.eventType in {"comment", "reply"})

    max_depth = 0.0
    max_burstiness = 0.0
    for event in events:
        if event.tree is not None and event.tree.depth is not None:
            max_depth = max(max_depth, float(event.tree.depth))
        if event.cascade is not None and event.cascade.burstiness is not None:
            max_burstiness = max(max_burstiness, float(event.cascade.burstiness))

    early_window_seconds = 300
    early_count = sum(
        1
        for index, _ in non_root
        if (_event_delta_seconds(events, index) or 10**9) <= early_window_seconds
    )
    if len(non_root) >= 3 and early_count >= max(2, int(len(non_root) * 0.55)):
        hints.append("Lan truyền diễn ra rất nhanh ngay sau khi đăng bài")

    if max_depth >= 2:
        hints.append("Tin được chia sẻ qua nhiều lớp (chuỗi share)")

    if share_count >= 3:
        hints.append("Có nhiều lượt chia sẻ trong thời gian ngắn")

    if max_burstiness >= 2.0:
        hints.append("Các tương tác đến theo cụm, không rải đều theo thời gian")

    if like_count + comment_count > 0 and share_count == 0 and len(non_root) >= 2:
        hints.append("Phản ứng chủ yếu quanh bài gốc, chưa lan truyền sâu")

    if like_count > share_count * 2 and share_count > 0:
        hints.append("Người dùng thích nhiều hơn là chia sẻ tiếp")

    if label == "REAL" and not hints and len(non_root) >= 2:
        hints.append("Cách lan truyền khá tự nhiên, tương tác rải đều theo thời gian")

    if label == "FAKE" and not hints and len(non_root) >= 2:
        hints.append("Một số tương tác trong chuỗi lan truyền làm tăng mức nghi ngờ")

    deduped: list[str] = []
    for hint in hints:
        if hint not in deduped:
            deduped.append(hint)
    return deduped[:3]


def build_narrative(
    label: str,
    fake_probability: float,
    graph_event_count: int,
    attributions: list[EventAttribution],
    context_hints: list[str],
) -> str:
    pct = fake_probability * 100
    parts: list[str] = []

    if context_hints:
        parts.append(f"{context_hints[0]}.")
    elif graph_event_count > 1:
        if label == "FAKE":
            parts.append("Hệ thống ghi nhận một số dấu hiệu bất thường trong cách bài viết lan truyền.")
        else:
            parts.append("Cách bài viết lan truyền không cho thấy dấu hiệu bất thường rõ ràng.")

    if attributions:
        top = attributions[0]
        if top.summary:
            parts.append(top.summary)
        if len(attributions) > 1 and attributions[1].summary:
            parts.append(f"Ngoài ra, {attributions[1].summary[0].lower() + attributions[1].summary[1:]}")

    if label == "FAKE":
        parts.append(
            f"Tổng cộng {graph_event_count} tương tác đã được phân tích "
            f"(mức nghi ngờ {pct:.0f}%)."
        )
    else:
        parts.append(
            f"Tổng cộng {graph_event_count} tương tác đã được phân tích "
            f"(độ tin cậy khoảng {100 - pct:.0f}%)."
        )

    return " ".join(parts)


def build_explanation_content(
    label: str,
    fake_probability: float,
    threshold: float,
    graph_event_count: int,
    events: list[InteractionEvent],
    attributions: list[EventAttribution],
    *,
    mode: str = "full",
) -> tuple[str, str, str, list[str]]:
    headline = build_headline(label, fake_probability)
    context_hints = detect_context_hints(events, label) if mode == "full" else []

    if mode == "text_only_fallback" or graph_event_count <= 1:
        explanation = build_basic_explanation(
            label,
            fake_probability,
            threshold,
            graph_event_count,
            mode=mode,
        )
        narrative = explanation
        return headline, narrative, explanation, context_hints

    narrative = build_narrative(
        label,
        fake_probability,
        graph_event_count,
        attributions,
        context_hints,
    )
    explanation = build_tige_explanation(
        label,
        fake_probability,
        threshold,
        graph_event_count,
        attributions,
    )
    return headline, narrative, explanation, context_hints


def _verdict_phrase(label: str, fake_probability: float) -> str:
    pct = fake_probability * 100
    if label == "FAKE":
        if pct >= 80:
            return f"Bài này có khả năng cao là tin giả ({pct:.0f}% nghi ngờ)"
        if pct >= 60:
            return f"Bài này có dấu hiệu tin giả ({pct:.0f}% nghi ngờ)"
        return f"Bài này có thể là tin giả ({pct:.0f}% nghi ngờ)"
    if pct <= 20:
        return f"Bài này có vẻ đáng tin ({100 - pct:.0f}% tin cậy)"
    if pct <= 40:
        return f"Bài này nhiều khả năng đáng tin ({100 - pct:.0f}% tin cậy)"
    return f"Bài này có thể đáng tin ({100 - pct:.0f}% tin cậy)"


def _impact_phrase(confidence_drop: float, target_class: int) -> str:
    supports_prediction = confidence_drop > 0
    if target_class == 1:
        if supports_prediction:
            return "khiến hệ thống nghi ngờ bài viết hơn"
        return "giảm bớt mức nghi ngờ"
    if supports_prediction:
        return "ủng hộ kết luận bài này đáng tin"
    return "làm giảm độ tin cậy vào bài viết"


def _event_summary(
    event: InteractionEvent,
    relative_time: str,
    score: TigeEventScore,
    target_class: int,
) -> str:
    actor = _actor_label(event)
    action = _action_verb(score.event_type)
    time_phrase = _natural_time_from_label(relative_time)
    level = _impact_level(score.tige_removal)
    impact = _impact_phrase(score.confidence_drop, target_class)

    if time_phrase == "ngay khi đăng bài":
        return (
            f"{actor.capitalize()} {action} {time_phrase} — tương tác này ảnh hưởng {level}, "
            f"{impact}."
        )
    return (
        f"{time_phrase.capitalize()}, {actor} {action} — tương tác này ảnh hưởng {level}, "
        f"{impact}."
    )


def build_event_attributions(
    tige_result: TigeResult,
    events: list[InteractionEvent],
    top_k: int,
) -> list[EventAttribution]:
    # Match notebook: rank by signed tige_entropy_gain (higher = more influential).
    ranked = sorted(
        tige_result.scores,
        key=lambda score: score.tige_removal,
        reverse=True,
    )[:top_k]

    attributions: list[EventAttribution] = []
    for score in ranked:
        event = events[score.event_index] if score.event_index < len(events) else None
        relative_time = _format_timeline_label(events, score.event_index)
        actor_label = _actor_label(event) if event is not None else "một người dùng"
        attributions.append(
            EventAttribution(
                eventIndex=score.event_index,
                eventType=score.event_type,
                eventTypeLabel=_event_type_label(score.event_type),
                relativeTime=relative_time,
                actorLabel=actor_label,
                tigeRemoval=round(score.tige_removal, 6),
                confidenceDrop=round(score.confidence_drop, 6),
                conditionalTige=(
                    round(score.conditional_tige, 6) if score.conditional_tige is not None else None
                ),
                summary=_event_summary(
                    event if event is not None else InteractionEvent(eventType=score.event_type),
                    relative_time,
                    score,
                    tige_result.target_class,
                ),
                impactLevel=_impact_level_code(score.tige_removal),
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
            key=lambda score: score.tige_removal,
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
                conditionalTige=(
                    round(score.conditional_tige, 6)
                    if score is not None and score.conditional_tige is not None
                    else None
                ),
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
    _ = threshold  # kept for API compatibility; hidden from user-facing text
    verdict = _verdict_phrase(label, fake_probability)
    base = f"{verdict}. Hệ thống đã phân tích {graph_event_count} tương tác lan truyền."

    if not attributions:
        return f"{base} Chưa xác định được tương tác nào ảnh hưởng đặc biệt đến kết luận."

    highlights = []
    for item in attributions[:3]:
        actor = item.actorLabel or "một người dùng"
        action = _action_verb(item.eventType)
        time_phrase = _natural_time_from_label(item.relativeTime or "t=0")
        if time_phrase == "ngay khi đăng bài":
            highlights.append(f"{actor} {action} {time_phrase}")
        else:
            highlights.append(f"{time_phrase} {actor} {action}")

    if len(highlights) == 1:
        signals = highlights[0]
    elif len(highlights) == 2:
        signals = f"{highlights[0]} và {highlights[1]}"
    else:
        signals = f"{highlights[0]}, {highlights[1]} và {highlights[2]}"

    top_summary = attributions[0].summary or ""
    return (
        f"{base} Các tương tác đáng chú ý nhất: {signals}. "
        f"Điểm then chốt: {top_summary}"
    )


def build_basic_explanation(
    label: str,
    fake_probability: float,
    threshold: float,
    graph_event_count: int,
    *,
    mode: str,
) -> str:
    _ = threshold
    verdict = _verdict_phrase(label, fake_probability)

    if mode == "text_only_fallback":
        return (
            f"{verdict}. Hiện chỉ phân tích được nội dung bài viết "
            "vì chưa có đủ tương tác lan truyền để đánh giá chi tiết hơn."
        )
    if graph_event_count <= 1:
        return (
            f"{verdict}. Cần thêm tương tác lan truyền "
            "trước khi hệ thống có thể chỉ ra các yếu tố ảnh hưởng cụ thể."
        )
    return f"{verdict}. Hệ thống đã phân tích {graph_event_count} tương tác lan truyền."
