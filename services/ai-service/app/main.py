from fastapi import FastAPI, HTTPException, status

from app.inference.predictor import ModelNotLoadedError, TGNNPredictor
from app.schemas import AnalyzeRequest, AnalyzeResponse, HealthResponse
from app.settings import settings

app = FastAPI(title="CyberSocial AI Service", version="1.0.0")
predictor = TGNNPredictor()


@app.get("/")
def root() -> dict[str, str]:
    return {
        "service": "CyberSocial AI Service",
        "endpoints": "/health, /analyze",
    }


@app.get("/health")
def health_check() -> HealthResponse:
    warnings = list(predictor.artifact.warnings) if predictor.artifact else []
    if predictor.load_error and not predictor.is_loaded:
        warnings.append(predictor.load_error)

    threshold = predictor.artifact.threshold if predictor.artifact else settings.threshold
    edge_normalization = (
        predictor.artifact.edge_mean is not None and predictor.artifact.edge_std is not None
        if predictor.artifact
        else False
    )
    st_model_name = (
        predictor.artifact.config.st_model_name if predictor.artifact else settings.st_model_name
    )
    num_users = predictor.artifact.num_users if predictor.artifact else None

    return HealthResponse(
        status="ok",
        modelLoaded=predictor.is_loaded,
        device=str(predictor.device),
        modelPath=str(settings.model_path),
        threshold=threshold,
        edgeNormalization=edge_normalization,
        stModelName=st_model_name,
        numUsers=num_users,
        warnings=warnings,
    )


@app.post("/analyze")
def analyze_text(request: AnalyzeRequest) -> AnalyzeResponse:
    try:
        return predictor.analyze(request)
    except ModelNotLoadedError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(exc),
        ) from exc
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc
