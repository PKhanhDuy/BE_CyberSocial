import secrets

from fastapi import Header, HTTPException, status

from app.settings import settings


def verify_analyze_api_key(x_api_key: str | None = Header(default=None, alias="X-API-Key")) -> None:
    expected = settings.api_key
    if not expected:
        return

    if not x_api_key or not secrets.compare_digest(x_api_key, expected):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing API key",
        )
