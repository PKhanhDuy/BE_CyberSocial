from fastapi import FastAPI
from pydantic import BaseModel


app = FastAPI(title="CyberSocial AI Service")


class AnalyzeRequest(BaseModel):
    text: str


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/analyze")
def analyze_text(request: AnalyzeRequest) -> dict[str, object]:
    # Placeholder endpoint. Replace this block with the real AI model pipeline.
    return {
        "label": "neutral",
        "score": 0.0,
        "inputLength": len(request.text),
    }
