# CyberSocial AI Service

FastAPI service for TGNN fake-news detection with TIGE explanations (Phase 3).

## Run Locally

```bash
cd BE_CyberSocial/services/ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Place the Kaggle export at `models/tgnn_deployment.pth` (see `MODEL_SPEC.md`).

## Docker Compose

From `BE_CyberSocial/`:

```bash
docker compose up --build ai-service
```

## API — Phase 1.4

### `GET /health`

```json
{
  "status": "ok",
  "modelLoaded": true,
  "device": "cpu",
  "modelPath": "models/tgnn_deployment.pth",
  "threshold": 0.59,
  "edgeNormalization": true,
  "stModelName": "sentence-transformers/all-MiniLM-L6-v2",
  "numUsers": 247933,
  "warnings": []
}
```

### `POST /analyze`

**Text-only (backend hiện tại — chế độ `text_only_fallback`):**

```json
{
  "text": "Article text to analyze",
  "includeXai": true
}
```

**Full TGNN với propagation events (chế độ `full`):**

```json
{
  "text": "Article text to analyze",
  "articleId": 12,
  "events": [
    {
      "srcUserId": 3,
      "dstArticleId": 12,
      "timestampUnix": 1719820800,
      "eventOrder": 0,
      "eventType": "share",
      "retweetCount": 2,
      "textLen": 120,
      "userProfile": {
        "logFollowers": 4.61,
        "logFollowing": 3.91,
        "logStatuses": 5.2,
        "accountCreatedUnix": 1609459200,
        "hasProfile": 1.0
      },
      "tree": { "depth": 1, "rootOutDegree": 3 },
      "cascade": { "speed": 0.2, "lifespan": 3600, "burstiness": 0.1 }
    }
  ],
  "includeXai": false
}
```

**Response:**

```json
{
  "fakeProbability": 0.72,
  "label": "FAKE",
  "riskLevel": "HIGH",
  "threshold": 0.59,
  "eventCount": 1,
  "explanation": "TGNN predicts FAKE with fake probability 0.720 at threshold 0.59 using 1 graph event(s).",
  "graphContribution": 0.41,
  "mode": "full",
  "tokenAttributions": [],
  "graphAttributions": [],
  "eventAttributions": []
}
```

| Field | Meaning |
| --- | --- |
| `mode` | `full` when `events` non-empty; `text_only_fallback` when only `text` is sent |
| `eventCount` | Number of client-supplied events (0 in text-only mode) |
| `graphContribution` | Mean fusion gate value (text vs graph balance) |

## Environment

```env
TGNN_MODEL_PATH=models/tgnn_deployment.pth
TGNN_ST_MODEL_NAME=sentence-transformers/all-MiniLM-L6-v2
TGNN_THRESHOLD=0.59
TGNN_ALLOW_TEXT_ONLY=true
TGNN_MAX_EVENTS=256
```

## Related Docs

- Feature contract: `MODEL_SPEC.md`
- Integration plan: `../../AI_TGNN_INTEGRATION_PLAN.md`
