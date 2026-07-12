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
    tige_temperature = float(os.getenv("TGNN_TIGE_TEMPERATURE", "1.0"))
    tige_top_k = int(os.getenv("TGNN_TIGE_TOP_K", "5"))
    tige_max_events = int(os.getenv("TGNN_TIGE_MAX_EVENTS", "32"))


settings = Settings()
