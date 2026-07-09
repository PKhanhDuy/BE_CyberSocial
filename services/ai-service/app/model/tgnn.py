import torch
import torch.nn as nn

from app.model.config import ModelConfig
from app.model.text_encoder import SentenceTransformerTextEncoder


class TemporalMemoryModule(nn.Module):
    def __init__(
        self,
        num_users: int,
        config: ModelConfig,
    ) -> None:
        super().__init__()
        self.use_user_embedding = config.use_user_embedding
        self.hidden_dim = config.hidden_dim

        input_dim = config.edge_feat_dim
        if self.use_user_embedding:
            self.user_emb = nn.Embedding(num_users, config.user_emb_dim, padding_idx=0)
            input_dim += config.user_emb_dim
        else:
            self.user_emb = None

        self.input_proj = nn.Sequential(
            nn.Linear(input_dim, config.hidden_dim),
            nn.ReLU(),
            nn.Dropout(config.dropout),
        )
        self.bidirectional = config.bidirectional_temporal
        self.gru = nn.GRU(
            input_size=config.hidden_dim,
            hidden_size=config.hidden_dim,
            batch_first=True,
            bidirectional=self.bidirectional,
        )
        self.gru_out_proj = (
            nn.Linear(config.hidden_dim * 2, config.hidden_dim)
            if self.bidirectional
            else nn.Identity()
        )

    def forward(self, user_ids: torch.Tensor, edge_feats: torch.Tensor) -> torch.Tensor:
        if self.use_user_embedding:
            assert self.user_emb is not None
            user_ids = user_ids.long()
            user_ids = torch.clamp(user_ids, min=0, max=self.user_emb.num_embeddings - 1)
            user_emb = self.user_emb(user_ids)
            features = torch.cat([user_emb, edge_feats], dim=-1)
        else:
            features = edge_feats

        projected = self.input_proj(features).unsqueeze(0)
        output, _ = self.gru(projected)
        output = self.gru_out_proj(output)
        return output.squeeze(0)


class TemporalAttentionAggregator(nn.Module):
    def __init__(self, config: ModelConfig) -> None:
        super().__init__()
        self.attn = nn.MultiheadAttention(
            embed_dim=config.hidden_dim,
            num_heads=config.temporal_num_heads,
            dropout=config.dropout,
            batch_first=True,
        )
        self.score = nn.Linear(config.hidden_dim, 1)
        self.norm = nn.LayerNorm(config.hidden_dim)
        self.dropout = nn.Dropout(config.dropout)

    def forward(self, event_h: torch.Tensor) -> torch.Tensor:
        x = event_h.unsqueeze(0)
        attn_out, _ = self.attn(x, x, x, need_weights=False)
        out = self.norm(x + self.dropout(attn_out))
        scores = self.score(out).squeeze(-1)
        weights = torch.softmax(scores, dim=-1)
        graph_h = torch.sum(weights.unsqueeze(-1) * out, dim=1)
        return graph_h.squeeze(0)


class TGNN(nn.Module):
    def __init__(
        self,
        num_users: int,
        config: ModelConfig,
        text_input_dim: int = 384,
    ) -> None:
        super().__init__()
        self.config = config
        self.text_encoder = SentenceTransformerTextEncoder(
            input_dim=text_input_dim,
            hidden_dim=config.hidden_dim,
            dropout=config.dropout,
        )
        self.memory = TemporalMemoryModule(num_users=num_users, config=config)
        self.aggregator = TemporalAttentionAggregator(config)
        self.fusion_gate = nn.Sequential(
            nn.Linear(config.hidden_dim * 3, config.hidden_dim),
            nn.ReLU(),
            nn.Dropout(config.dropout),
            nn.Linear(config.hidden_dim, config.hidden_dim),
            nn.Sigmoid(),
        )
        self.classifier = nn.Sequential(
            nn.Linear(config.hidden_dim, config.hidden_dim),
            nn.ReLU(),
            nn.Dropout(config.dropout),
            nn.Linear(config.hidden_dim, 2),
        )

    def forward_logits(
        self,
        text_h: torch.Tensor,
        user_idx: torch.Tensor,
        edge_feats: torch.Tensor,
    ) -> tuple[torch.Tensor, torch.Tensor]:
        if text_h.dim() == 1:
            text_h = text_h.unsqueeze(0)

        event_h = self.memory(user_idx, edge_feats)
        h_graph = self.aggregator(event_h).unsqueeze(0)

        fusion_input = torch.cat([text_h, h_graph, torch.abs(text_h - h_graph)], dim=-1)
        gate = self.fusion_gate(fusion_input)
        fused = gate * text_h + (1.0 - gate) * h_graph
        logits = self.classifier(fused)
        return logits, gate
