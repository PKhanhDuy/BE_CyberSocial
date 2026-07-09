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
    return _clean_state_dict(state_dict)


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


def load_artifact(model_path: Path | None = None, device: torch.device | None = None) -> LoadedArtifact:
    path = model_path or settings.model_path
    if not path.exists():
        raise FileNotFoundError(f"TGNN checkpoint not found at {path}")

    resolved_device = device or torch.device("cuda" if torch.cuda.is_available() else "cpu")
    payload = torch.load(path, map_location=resolved_device)
    if not isinstance(payload, dict):
        raise ValueError("Checkpoint must be a dictionary payload")

    state_dict = _extract_state_dict(payload)
    config = ModelConfig.from_dict(payload.get("cfg"))
    num_users = int(payload.get("num_users") or len(payload.get("user2idx") or {}) or 1)
    text_input_dim = _infer_text_input_dim(state_dict)

    model = TGNN(
        num_users=num_users,
        config=config,
        text_input_dim=text_input_dim,
    ).to(resolved_device)
    model.load_state_dict(state_dict, strict=False)
    model.eval()

    if not config.use_pretrained_text:
        raise ValueError("Only use_pretrained_text=true checkpoints are supported in ai-service")

    sentence_transformer = SentenceTransformer(config.st_model_name, device=str(resolved_device))

    warnings: list[str] = []
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

    threshold_value = payload.get("threshold")
    threshold = float(threshold_value if threshold_value is not None else settings.threshold)

    feature_names = payload.get("EDGE_FEATURE_NAMES")
    resolved_feature_names = resolve_feature_names(list(feature_names) if feature_names else None)

    return LoadedArtifact(
        model=model,
        config=config,
        sentence_transformer=sentence_transformer,
        threshold=threshold,
        num_users=num_users,
        user2idx={str(key): int(value) for key, value in (payload.get("user2idx") or {}).items()},
        event_type2idx={
            str(key): int(value) for key, value in (payload.get("event_type2idx") or {}).items()
        },
        edge_feature_names=resolved_feature_names,
        edge_mean=edge_mean,
        edge_std=edge_std,
        text_input_dim=text_input_dim,
        warnings=warnings,
    )
