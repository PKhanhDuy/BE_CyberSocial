import torch

from app.schemas import AnalyzeRequest, InteractionEvent


def build_user_index_tensor(
    events: list[InteractionEvent],
    user2idx: dict[str, int],
    device: torch.device,
) -> torch.Tensor:
    if not events:
        return torch.zeros(1, dtype=torch.long, device=device)

    indices: list[int] = []
    for event in events:
        user_key = str(event.srcUserId)
        if user_key in user2idx:
            indices.append(int(user2idx[user_key]))
        elif event.srcUserId > 0:
            indices.append(int(event.srcUserId))
        else:
            indices.append(0)

    return torch.tensor(indices, dtype=torch.long, device=device)
