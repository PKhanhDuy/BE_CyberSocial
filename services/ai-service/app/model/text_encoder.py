import torch
import torch.nn as nn


class SentenceTransformerTextEncoder(nn.Module):
    """Project frozen SentenceTransformer embeddings to hidden_dim."""

    def __init__(self, input_dim: int, hidden_dim: int, dropout: float) -> None:
        super().__init__()
        self.proj = nn.Sequential(
            nn.Linear(input_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(dropout),
        )

    def forward(self, text_embedding: torch.Tensor) -> torch.Tensor:
        if text_embedding.dim() == 1:
            text_embedding = text_embedding.unsqueeze(0)
        return self.proj(text_embedding)
