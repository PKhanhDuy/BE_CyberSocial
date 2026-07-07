import os
from pathlib import Path


class Settings:
    model_path = Path(os.getenv("TGNN_MODEL_PATH", "models/tgnn_deployment.pth"))
    st_model_name = os.getenv(
        "TGNN_ST_MODEL_NAME",
        "sentence-transformers/all-MiniLM-L6-v2",
    )
    max_len = int(os.getenv("TGNN_MAX_LEN", "256"))
    threshold = float(os.getenv("TGNN_THRESHOLD", "0.5"))
    max_events_per_article = int(os.getenv("TGNN_MAX_EVENTS", "256"))
    allow_text_only = os.getenv("TGNN_ALLOW_TEXT_ONLY", "true").lower() == "true"


settings = Settings()
