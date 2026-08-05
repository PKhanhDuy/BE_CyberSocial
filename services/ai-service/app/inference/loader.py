from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import numpy as np
import torch
from sentence_transformers import SentenceTransformer

from app.inference.features import EDGE_FEATURE_NAMES, resolve_feature_names
from app.model.config import ModelConfig
from app.model.tgnn import TGNN
from app.settings import settings


@dataclass
class LoadedArtifact:
    model: TGNN
    config: ModelConfig
    sentence_transformer: SentenceTransformer
    threshold: float
    num_users: int
    tige_temperature: float = 1.0
    user2idx: dict[str, int] = field(default_factory=dict)
    event_type2idx: dict[str, int] = field(default_factory=dict)
    edge_feature_names: list[str] = field(default_factory=lambda: list(EDGE_FEATURE_NAMES))
    edge_mean: np.ndarray | None = None
    edge_std: np.ndarray | None = None
    text_input_dim: int = 384
    warnings: list[str] = field(default_factory=list)


def _clean_state_dict(state_dict: dict[str, torch.Tensor]) -> dict[str, torch.Tensor]:
    cleaned: dict[str, torch.Tensor] = {}
    for key, value in state_dict.items():
        if not torch.is_tensor(value):
            continue
        new_key = key
        for prefix in ("_orig_mod.", "module."):
            if new_key.startswith(prefix):
                new_key = new_key[len(prefix) :]
        cleaned[new_key] = value
    return cleaned


def _extract_state_dict(payload: dict[str, Any]) -> dict[str, torch.Tensor]:
    state_dict = payload.get("model_state_dict") or payload.get("state_dict") or payload
    if not isinstance(state_dict, dict):
        raise ValueError("Checkpoint does not contain a valid state_dict")
    # Notebook export may nest tensors under model_state_dict; ignore non-tensor metadata.
    if "model_state_dict" not in payload and "state_dict" not in payload:
        # Raw state_dict-only file: keep tensor entries only.
        state_dict = {key: value for key, value in state_dict.items() if torch.is_tensor(value)}
    return _clean_state_dict(state_dict)


def _extract_config(payload: dict[str, Any]) -> dict[str, Any] | None:
    """Notebook saves `CFG`; deployment export may use `cfg`."""
    config = payload.get("cfg") or payload.get("CFG") or payload.get("config")
    return config if isinstance(config, dict) else None


def _infer_text_input_dim(state_dict: dict[str, torch.Tensor]) -> int:
    weight = state_dict.get("text_encoder.proj.0.weight")
    if weight is None:
        return 384
    return int(weight.shape[1])


def _to_numpy_array(value: Any) -> np.ndarray | None:
    if value is None:
        return None
    if torch.is_tensor(value):
        return value.detach().cpu().numpy().astype(np.float32)
    return np.asarray(value, dtype=np.float32)


def _resolve_threshold(payload: dict[str, Any], config: dict[str, Any] | None) -> float:
    for key in ("threshold", "best_thr", "best_threshold"):
        if payload.get(key) is not None:
            return float(payload[key])
        if config and config.get(key) is not None:
            return float(config[key])
    return float(settings.threshold)


def _resolve_tige_temperature(payload: dict[str, Any], config: dict[str, Any] | None) -> float:
    for key in ("tige_temperature", "T_robust", "tige_temp"):
        if payload.get(key) is not None:
            return float(payload[key])
        if config and config.get(key) is not None:
            return float(config[key])
    return float(settings.tige_temperature)


def _resolve_event_type2idx(payload: dict[str, Any], config: dict[str, Any] | None) -> dict[str, int]:
    raw = payload.get("event_type2idx")
    if not isinstance(raw, dict) and config:
        raw = config.get("event_type2idx")
    if not isinstance(raw, dict):
        return {}
    return {str(key): int(value) for key, value in raw.items()}


def load_artifact(model_path: Path | None = None, device: torch.device | None = None) -> LoadedArtifact:
    path = model_path or settings.model_path
    if not path.exists():
        raise FileNotFoundError(f"TGNN checkpoint not found at {path}")

    resolved_device = device or torch.device("cuda" if torch.cuda.is_available() else "cpu")
    # Notebook / deploy checkpoints include numpy arrays; trusted local artifact.
    payload = torch.load(path, map_location=resolved_device, weights_only=False)
    if not isinstance(payload, dict):
        raise ValueError("Checkpoint must be a dictionary payload")

    state_dict = _extract_state_dict(payload)
    raw_config = _extract_config(payload)
    config = ModelConfig.from_dict(raw_config)
    user2idx_raw = payload.get("user2idx") or {}
    num_users = int(
        payload.get("num_users")
        or (len(user2idx_raw) if isinstance(user2idx_raw, dict) and user2idx_raw else 0)
        or 1
    )
    text_input_dim = _infer_text_input_dim(state_dict)

    model = TGNN(
        num_users=num_users,
        config=config,
        text_input_dim=text_input_dim,
    ).to(resolved_device)
    incompatible = model.load_state_dict(state_dict, strict=False)
    model.eval()

    if not config.use_pretrained_text:
        raise ValueError("Only use_pretrained_text=true checkpoints are supported in ai-service")

    sentence_transformer = SentenceTransformer(config.st_model_name, device=str(resolved_device))

    warnings: list[str] = []
    if incompatible.missing_keys:
        warnings.append(f"Missing keys when loading checkpoint: {incompatible.missing_keys[:8]}")
    if incompatible.unexpected_keys:
        warnings.append(f"Unexpected keys in checkpoint: {incompatible.unexpected_keys[:8]}")

    edge_mean = _to_numpy_array(payload.get("EDGE_MEAN"))
    edge_std = _to_numpy_array(payload.get("EDGE_STD"))
    if edge_mean is None or edge_std is None:
        normalization = payload.get("normalization") or {}
        edge_mean = _to_numpy_array(normalization.get("means"))
        edge_std = _to_numpy_array(normalization.get("stds"))
    if edge_mean is None or edge_std is None:
        warnings.append(
            "EDGE_MEAN/EDGE_STD missing from checkpoint; edge features will not be z-score normalized"
        )
    elif edge_mean.shape[-1] != 17 or edge_std.shape[-1] != 17:
        warnings.append(
            f"EDGE_MEAN/EDGE_STD shape mismatch: mean={edge_mean.shape}, std={edge_std.shape}"
        )

    threshold = _resolve_threshold(payload, raw_config)
    if payload.get("threshold") is None and payload.get("best_thr") is None:
        warnings.append(
            f"threshold/best_thr missing in checkpoint; using TGNN_THRESHOLD={threshold}"
        )

    tige_temperature = _resolve_tige_temperature(payload, raw_config)
    event_type2idx = _resolve_event_type2idx(payload, raw_config)
    if not event_type2idx:
        warnings.append("event_type2idx missing; falling back to CyberSocial default mapping")

    feature_names = payload.get("EDGE_FEATURE_NAMES")
    if feature_names is None and raw_config:
        feature_names = raw_config.get("EDGE_FEATURE_NAMES")
    resolved_feature_names = resolve_feature_names(list(feature_names) if feature_names else None)

    return LoadedArtifact(
        model=model,
        config=config,
        sentence_transformer=sentence_transformer,
        threshold=threshold,
        tige_temperature=tige_temperature,
        num_users=num_users,
        user2idx={str(key): int(value) for key, value in (user2idx_raw or {}).items()},
        event_type2idx=event_type2idx,
        edge_feature_names=resolved_feature_names,
        edge_mean=edge_mean,
        edge_std=edge_std,
        text_input_dim=text_input_dim,
        warnings=warnings,
    )
