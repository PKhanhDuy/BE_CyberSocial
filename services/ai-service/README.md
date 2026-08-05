# CyberSocial AI Service

FastAPI service for TGNN fake-news detection with TIGE explanations (Phase 3).

## Run Locally

```bash
cd BE_CyberSocial/services/ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Place the trained checkpoint at `models/tgnn_tice.pt` (see `MODEL_SPEC.md` / `models/README.md`).

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
  "modelPath": "models/tgnn_tice.pt",
  "threshold": 0.5,
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
TGNN_MODEL_PATH=models/tgnn_tice.pt
TGNN_ST_MODEL_NAME=sentence-transformers/all-MiniLM-L6-v2
TGNN_THRESHOLD=0.59
TGNN_ALLOW_TEXT_ONLY=true
TGNN_MAX_EVENTS=256
```

## LLM narration — giải thích tự nhiên hơn (tùy chọn)

TGNN/TIGE vẫn quyết định kết quả. LLM chỉ **viết lại** `headline` và `narrative` từ dữ liệu có sẵn.

Chọn provider qua `LLM_NARRATION_PROVIDER`: `none` | `gemini` | `ollama`

### Gemini (free — khuyên dùng khi deploy)

1. Lấy API key miễn phí tại [Google AI Studio](https://aistudio.google.com/apikey)
2. Thêm vào `.env`:

```env
LLM_NARRATION_PROVIDER=gemini
GEMINI_API_KEY=your-api-key-here
GEMINI_MODEL=gemini-2.0-flash
GEMINI_TIMEOUT_SECONDS=30
```

3. Restart AI service. Lỗi / hết quota → tự động fallback template.

Model free khác: `gemini-1.5-flash` (nhẹ hơn, ít quota hơn).

**Lưu ý:** Không commit `GEMINI_API_KEY` lên Git. Chỉ đặt trong `.env` trên máy hoặc secret của VPS.

### Ollama (local)

```env
LLM_NARRATION_PROVIDER=ollama
OLLAMA_BASE_URL=http://127.0.0.1:11434
OLLAMA_MODEL=qwen2.5:7b
```

Hoặc legacy: `OLLAMA_NARRATION_ENABLED=true` (tương đương `LLM_NARRATION_PROVIDER=ollama`).

Docker → host: `OLLAMA_BASE_URL=http://host.docker.internal:11434`

### Tắt LLM

```env
LLM_NARRATION_PROVIDER=none
```

## Related Docs

- Feature contract: `MODEL_SPEC.md`
- Integration plan: `../../AI_TGNN_INTEGRATION_PLAN.md`
