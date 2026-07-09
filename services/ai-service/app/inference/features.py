import math

import numpy as np

from app.schemas import AnalyzeRequest, InteractionEvent
from app.settings import settings

EDGE_FEATURE_NAMES: list[str] = [
    "relative_time_sec",
    "delta_t_sec",
    "retweet_count",
    "event_order_in_article",
    "text_len",
    "event_type_idx",
    "u_log_followers",
    "u_log_following",
    "u_log_statuses",
    "u_foll_ratio",
    "u_acct_age_log",
    "u_has_profile",
    "t_depth",
    "t_root_outdeg",
    "c_speed",
    "c_lifespan",
    "c_burstiness",
]

DEFAULT_EVENT_TYPE2IDX: dict[str, int] = {
    "tweet": 0,
    "retweet": 1,
    "reply": 2,
    "quote": 3,
    "like": 4,
    "comment": 2,
    "share": 1,
}


def _log1p(value: float) -> float:
    return float(math.log1p(max(value, 0.0)))


def _compute_cascade(raw_relative_times: list[float], event_count: int) -> dict[str, float]:
    if not raw_relative_times:
        return {"c_speed": 0.0, "c_lifespan": 0.0, "c_burstiness": 0.0}

    max_relative = max(raw_relative_times)
    c_speed = _log1p(event_count / (max_relative / 3600.0 + 1.0))
    c_lifespan = _log1p(max_relative)

    sorted_times = sorted(raw_relative_times)
    gaps = [sorted_times[index + 1] - sorted_times[index] for index in range(len(sorted_times) - 1)]
    if len(gaps) < 2:
        burstiness = 0.0
    else:
        gap_array = np.asarray(gaps, dtype=np.float64)
        burstiness = float(gap_array.std() / (gap_array.mean() + 1e-6))
    burstiness = float(np.clip(burstiness, 0.0, 5.0))
    return {"c_speed": c_speed, "c_lifespan": c_lifespan, "c_burstiness": burstiness}


def _profile_values(event: InteractionEvent, timestamp_unix: float | None) -> dict[str, float]:
    profile = event.userProfile
    if profile is None:
        return {
            "u_log_followers": 0.0,
            "u_log_following": 0.0,
            "u_log_statuses": 0.0,
            "u_foll_ratio": 0.0,
            "u_acct_age_log": 0.0,
            "u_has_profile": 0.0,
        }

    followers = float(profile.logFollowers or 0.0)
    following = float(profile.logFollowing or 0.0)
    statuses = float(profile.logStatuses or 0.0)
    age_log = 0.0
    if profile.accountCreatedUnix is not None and timestamp_unix is not None:
        age_days = max((timestamp_unix - profile.accountCreatedUnix) / 86400.0, 0.0)
        age_log = _log1p(age_days)

    return {
        "u_log_followers": followers,
        "u_log_following": following,
        "u_log_statuses": statuses,
        "u_foll_ratio": followers - following,
        "u_acct_age_log": age_log,
        "u_has_profile": float(profile.hasProfile or 0.0),
    }


def _tree_values(event: InteractionEvent) -> dict[str, float]:
    tree = event.tree
    if tree is None:
        return {"t_depth": 0.0, "t_root_outdeg": 0.0}
    return {
        "t_depth": _log1p(float(tree.depth or 0.0)),
        "t_root_outdeg": _log1p(float(tree.rootOutDegree or 0.0)),
    }


def build_raw_feature_rows(
    request: AnalyzeRequest,
    event_type2idx: dict[str, int] | None = None,
) -> list[dict[str, float]]:
    mapping = event_type2idx or DEFAULT_EVENT_TYPE2IDX
    events = list(request.events)
    if not events:
        events = [InteractionEvent(dstArticleId=request.articleId, textLen=len(request.text))]

    sorted_events = sorted(
        events,
        key=lambda event: event.timestampUnix if event.timestampUnix is not None else 0.0,
    )[: settings.max_events_per_article]
    min_timestamp = min(
        (event.timestampUnix for event in sorted_events if event.timestampUnix is not None),
        default=None,
    )

    raw_relative_times: list[float] = []
    rows: list[dict[str, float]] = []
    previous_relative = 0.0

    for index, event in enumerate(sorted_events):
        relative_time = 0.0
        if min_timestamp is not None and event.timestampUnix is not None:
            relative_time = max(float(event.timestampUnix) - float(min_timestamp), 0.0)

        delta_t = relative_time - previous_relative if index > 0 else relative_time
        previous_relative = relative_time
        raw_relative_times.append(relative_time)

        event_order = float(event.eventOrder if event.eventOrder is not None else index)
        text_len = float(event.textLen if event.textLen is not None else len(request.text))
        event_type_idx = float(mapping.get(event.eventType, 0))

        row: dict[str, float] = {
            "relative_time_sec": _log1p(relative_time),
            "delta_t_sec": _log1p(max(delta_t, 0.0)),
            "retweet_count": _log1p(float(event.retweetCount)),
            "event_order_in_article": event_order,
            "text_len": _log1p(text_len),
            "event_type_idx": event_type_idx,
        }
        row.update(_profile_values(event, event.timestampUnix))
        row.update(_tree_values(event))
        rows.append(row)

    cascade = _compute_cascade(raw_relative_times, len(rows))
    cascade_override = next((event.cascade for event in sorted_events if event.cascade), None)
    if cascade_override is not None:
        cascade = {
            "c_speed": float(cascade_override.speed or cascade["c_speed"]),
            "c_lifespan": float(cascade_override.lifespan or cascade["c_lifespan"]),
            "c_burstiness": float(cascade_override.burstiness or cascade["c_burstiness"]),
        }
    for row in rows:
        row.update(cascade)

    return rows


def rows_to_matrix(rows: list[dict[str, float]]) -> np.ndarray:
    return np.asarray(
        [[row[name] for name in EDGE_FEATURE_NAMES] for row in rows],
        dtype=np.float32,
    )


def normalize_features(
    raw_matrix: np.ndarray,
    edge_mean: np.ndarray | None,
    edge_std: np.ndarray | None,
) -> np.ndarray:
    if edge_mean is None or edge_std is None:
        return raw_matrix

    mean = np.asarray(edge_mean, dtype=np.float32)
    std = np.asarray(edge_std, dtype=np.float32)
    return (raw_matrix - mean) / std


def build_edge_matrix(
    request: AnalyzeRequest,
    edge_mean: np.ndarray | None,
    edge_std: np.ndarray | None,
    event_type2idx: dict[str, int] | None = None,
) -> np.ndarray:
    rows = build_raw_feature_rows(request, event_type2idx=event_type2idx)
    raw_matrix = rows_to_matrix(rows)
    return normalize_features(raw_matrix, edge_mean, edge_std)


def resolve_feature_names(custom_names: list[str] | None) -> list[str]:
    if custom_names:
        if list(custom_names) != EDGE_FEATURE_NAMES:
            raise ValueError("EDGE_FEATURE_NAMES in artifact does not match MODEL_SPEC order")
        return list(custom_names)
    return EDGE_FEATURE_NAMES
