from dataclasses import dataclass


@dataclass
class ModelConfig:
    hidden_dim: int = 64
    text_emb_dim: int = 64
    user_emb_dim: int = 2
    edge_feat_dim: int = 17
    temporal_num_heads: int = 4
    bidirectional_temporal: bool = True
    dropout: float = 0.35
    max_len: int = 256
    use_pretrained_text: bool = True
    st_model_name: str = "sentence-transformers/all-MiniLM-L6-v2"
    use_user_embedding: bool = False

    @classmethod
    def from_dict(cls, values: dict | None) -> "ModelConfig":
        if not values:
            return cls()

        return cls(
            hidden_dim=int(values.get("hidden_dim", 64)),
            text_emb_dim=int(values.get("text_emb_dim", 64)),
            user_emb_dim=int(values.get("user_emb_dim", 2)),
            edge_feat_dim=int(values.get("edge_feat_dim", 17)),
            temporal_num_heads=int(values.get("temporal_num_heads", 4)),
            bidirectional_temporal=bool(values.get("bidirectional_temporal", True)),
            dropout=float(values.get("dropout", 0.35)),
            max_len=int(values.get("max_len", 256)),
            use_pretrained_text=bool(values.get("use_pretrained_text", True)),
            st_model_name=str(
                values.get("st_model_name", "sentence-transformers/all-MiniLM-L6-v2")
            ),
            use_user_embedding=bool(values.get("use_user_embedding", False)),
        )
