import os
from pathlib import Path


class Settings:
    model_path = Path(os.getenv("TGNN_MODEL_PATH", "models/tgnn_tice.pt"))
    api_key = os.getenv("AI_SERVICE_API_KEY", "").strip()
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
    tige_max_events = int(os.getenv("TGNN_TIGE_MAX_EVENTS", "100"))

    ollama_narration_enabled = os.getenv("OLLAMA_NARRATION_ENABLED", "false").lower() == "true"
    ollama_base_url = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434")
    ollama_model = os.getenv("OLLAMA_MODEL", "qwen2.5:7b")
    ollama_timeout_seconds = int(os.getenv("OLLAMA_TIMEOUT_SECONDS", "30"))

    gemini_api_key = os.getenv("GEMINI_API_KEY", "")
    gemini_model = os.getenv("GEMINI_MODEL", "gemini-2.0-flash")
    gemini_timeout_seconds = int(os.getenv("GEMINI_TIMEOUT_SECONDS", "30"))

    @property
    def llm_narration_provider(self) -> str:
        explicit = os.getenv("LLM_NARRATION_PROVIDER", "").strip().lower()
        if explicit:
            return explicit
        if self.ollama_narration_enabled:
            return "ollama"
        return "none"


settings = Settings()
