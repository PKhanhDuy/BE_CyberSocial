# MODEL_SPEC — TGNN (`tgnn-tice.ipynb`)

Tài liệu contract inference cho AI service CyberSocial.

- Notebook huấn luyện: `services/ai-service/tgnn-tice.ipynb`
- Weights hiện tại: `models/tgnn_model.pth` (chưa đủ metadata deploy)
- Bundle deploy khuyến nghị: `models/tgnn_deployment.pth` (cần export thêm `EDGE_MEAN`, `EDGE_STD`, `threshold`)

---

## 1. Tổng quan pipeline

```text
Article text
  → SentenceTransformer (all-MiniLM-L6-v2, 384d)
  → text_encoder.proj → 64d

Event stream (T events × 17 features)
  → log1p (một số cột) → z-score normalize
  → TemporalMemoryModule (BiGRU)
  → TemporalAttentionAggregator
  → 64d graph embedding

Fusion gate(text, graph) → classifier → logits [REAL, FAKE]
```

Cấu hình model (`cfg` trong checkpoint):

| Key | Giá trị |
|---|---|
| `hidden_dim` | 64 |
| `text_emb_dim` | 64 |
| `edge_feat_dim` | **17** |
| `use_user_embedding` | **false** |
| `use_user_stat_features` | **false** |
| `st_model_name` | `sentence-transformers/all-MiniLM-L6-v2` |
| `bidirectional_temporal` | true |
| `temporal_num_heads` | 4 |
| `dropout` | 0.35 |
| `max_len` | 256 |

---

## 2. Danh sách 17 edge features (đúng thứ tự)

Thứ tự vector `edge_feats[t]` tại event thứ `t` (index 0-based):

| # | Tên feature | Mô tả ngắn |
|---|---|---|
| 0 | `relative_time_sec` | Thời gian event tính từ event đầu tiên của bài (giây) |
| 1 | `delta_t_sec` | Khoảng cách thời gian với event liền trước (giây) |
| 2 | `retweet_count` | Số retweet tại thời điểm event (FakeNewsNet) |
| 3 | `event_order_in_article` | Thứ tự event trong bài (0, 1, 2, …) |
| 4 | `text_len` | Độ dài text event (ký tự/token length) |
| 5 | `event_type_idx` | Loại event (integer index, xem mục 4) |
| 6 | `u_log_followers` | Log followers user (từ `users.csv`) |
| 7 | `u_log_following` | Log following user |
| 8 | `u_log_statuses` | Log số trạng thái user |
| 9 | `u_foll_ratio` | `u_log_followers - u_log_following` |
| 10 | `u_acct_age_log` | `log1p(tuổi tài khoản tại event, ngày)` |
| 11 | `u_has_profile` | 0/1 — user có profile hay không |
| 12 | `t_depth` | Độ sâu node trong cây retweet (đã log1p) |
| 13 | `t_root_outdeg` | Out-degree của root tweet (đã log1p) |
| 14 | `c_speed` | Tốc độ lan truyền cascade (event/giờ, log1p) |
| 15 | `c_lifespan` | Thời gian sống cascade (log1p max relative time) |
| 16 | `c_burstiness` | std/mean khoảng cách inter-event (clip 0–5) |

**Lưu ý:**

- `t_n_children` bị loại (`_DEAD_FEATURES`) — **không** nằm trong 17 feature.
- `USER_STAT_FEATURES` (`user_total_log`, `user_fake_ratio`, `user_purity`) **không** dùng khi `use_user_stat_features=false` (mặc định notebook).

Công thức lắp vector (notebook):

```python
EDGE_FEATURE_NAMES = (
    BASE_EDGE_FEATURES          # 5 cột
    + ["event_type_idx"]        # 1 cột
    + USER_STAT_FEATURES        # rỗng khi use_user_stat_features=False
    + EXTRA_EDGE_FEATURES       # profile + tree (không t_n_children) + cascade
)
# => edge_feat_dim = 17
```

---

## 3. Quy tắc `log1p` theo từng cột

### 3.1. Base features (`prepare_edge_frame`)

Áp dụng **trước** khi fit `EDGE_MEAN` / `EDGE_STD`:

```python
for c in BASE_EDGE_FEATURES:
    df[c] = pd.to_numeric(df[c], errors="coerce").fillna(0.0)
    if c in [
        "relative_time_sec",
        "delta_t_sec",
        "retweet_count",
        "favorite_count",   # không dùng trong 17-d vector (dead trên dataset)
        "text_len",
        "user_total_log",     # chỉ khi bật user stat features
    ]:
        df[c] = np.log1p(np.maximum(df[c], 0))
```

| Feature | `log1p` tại bước này? |
|---|---|
| `relative_time_sec` | **Có** |
| `delta_t_sec` | **Có** |
| `retweet_count` | **Có** |
| `event_order_in_article` | **Không** |
| `text_len` | **Có** |
| `event_type_idx` | **Không** (integer index) |

### 3.2. Profile features (section 5b)

| Feature | Cách tính / scale |
|---|---|
| `u_log_followers` | Lấy sẵn `log_followers_count` từ users.csv (đã log) |
| `u_log_following` | Lấy sẵn `log_following_count` |
| `u_log_statuses` | Lấy sẵn `log_statuses_count` |
| `u_foll_ratio` | `u_log_followers - u_log_following` |
| `u_acct_age_log` | `log1p(max(0, (event_unix - acct_created_unix) / 86400))` |
| `u_has_profile` | 0 hoặc 1 (không log1p) |

Thiếu profile → fill `0.0`.

### 3.3. Tree features (section 5b)

| Feature | Cách tính / scale |
|---|---|
| `t_depth` | `log1p(depth trong cây retweet)` |
| `t_root_outdeg` | `log1p(out-degree của root tweet)` |

### 3.4. Cascade features (section 5b, gắn theo article)

Tính trên toàn bộ event của bài, gán cùng giá trị cho mọi event trong bài:

| Feature | Công thức |
|---|---|
| `c_speed` | `log1p(count_events / (max_relative_time_sec / 3600 + 1))` |
| `c_lifespan` | `log1p(max_relative_time_sec)` |
| `c_burstiness` | `std(inter_event_gaps) / (mean(inter_event_gaps) + 1e-6)`, clip `[0, 5]` |

---

## 4. `event_type_idx`

Map từ cột loại event (notebook: `event_type`) sang integer, fit **trên train split**:

```python
event_type2idx = {v: i for i, v in enumerate(sorted(event_types))}
event_type_idx = event_type2idx.get(event_type_str, 0)  # unknown → 0
```

CyberSocial mapping gợi ý (cần lưu dict khi export artifact):

| CyberSocial event | Gợi ý map |
|---|---|
| `post` / `tweet` | index tương ứng `tweet` |
| `share` / `retweet` | index tương ứng `retweet` |
| `comment` / `reply` | index tương ứng `reply` |
| `quote` | index tương ứng `quote` |
| unknown | `0` |

---

## 5. Chuẩn hóa (normalize)

Fit **chỉ trên train edges** (notebook):

```python
train_edge_matrix = train_edge_df[EDGE_FEATURE_NAMES].astype(float).values
EDGE_MEAN = train_edge_matrix.mean(axis=0)   # shape [17]
EDGE_STD  = train_edge_matrix.std(axis=0) + 1e-6
```

Tại inference (mỗi event, mỗi feature):

```python
edge_normalized = (edge_raw - EDGE_MEAN) / EDGE_STD
```

**Feature thiếu trên CyberSocial:** dùng giá trị raw = train mean → sau normalize = **0.0**.

TIGE mask baseline trong notebook cũng dùng vector **0** trong không gian đã chuẩn hóa.

> `EDGE_MEAN` và `EDGE_STD` **chưa có** trong `tgnn_model.pth` hiện tại. Bắt buộc export vào `tgnn_deployment.pth` trước khi deploy production.

---

## 6. Threshold phân loại

Sau khi train, notebook chọn threshold trên **validation set** (maximize F1):

```python
best_thr = 0.5
best_f1 = -1.0

for thr in np.arange(0.10, 0.90, 0.01):
    pred = (prob_val >= thr).astype(int)
    f1 = f1_score(y_val, pred)
    if f1 > best_f1:
        best_f1 = f1
        best_thr = thr
```

Quy tắc classify tại inference:

```python
prob_fake = softmax(logits)[1]   # class index 1 = FAKE
label = "FAKE" if prob_fake >= threshold else "REAL"
```

| Class index | Label |
|---|---|
| 0 | REAL |
| 1 | FAKE |

**Risk level gợi ý cho API** (service layer, không nằm trong notebook):

| `fakeProbability` | `riskLevel` |
|---|---|
| ≥ 0.75 | HIGH |
| ≥ 0.45 | MEDIUM |
| < 0.45 | LOW |

> `threshold` **chưa có** trong `tgnn_model.pth`. Export từ notebook hoặc set `TGNN_THRESHOLD` trong `.env` làm fallback.

---

## 7. Giới hạn và sắp xếp events

- Sắp xếp events theo thời gian tăng dần trước khi build features.
- Giới hạn tối đa: `max_events_per_article = 256` (notebook `CFG`).
- Khuyến nghị API: cap 100 events nếu cần giảm latency TIGE.

---

## 8. Input mẫu — 1 bài viết

### 8.1. Request API (draft)

```json
{
  "text": "Breaking: scientists discover miracle cure ignored by mainstream media.",
  "articleId": "post-1024",
  "events": [
    {
      "timestampUnix": 1719820800.0,
      "eventOrder": 0,
      "eventType": "tweet",
      "retweetCount": 0,
      "textLen": 72,
      "userProfile": {
        "logFollowers": 8.2,
        "logFollowing": 6.1,
        "logStatuses": 9.5,
        "accountCreatedUnix": 1451606400.0,
        "hasProfile": 1
      },
      "tree": { "depth": 0, "rootOutDegree": 0 },
      "cascade": { "speed": 0.0, "lifespan": 0.0, "burstiness": 0.0 }
    },
    {
      "timestampUnix": 1719821100.0,
      "eventOrder": 1,
      "eventType": "retweet",
      "retweetCount": 12,
      "textLen": 0,
      "userProfile": {
        "logFollowers": 10.1,
        "logFollowing": 7.0,
        "logStatuses": 11.2,
        "accountCreatedUnix": 1400000000.0,
        "hasProfile": 1
      },
      "tree": { "depth": 1, "rootOutDegree": 3 },
      "cascade": { "speed": 1.2, "lifespan": 5.0, "burstiness": 0.8 }
    },
    {
      "timestampUnix": 1719821700.0,
      "eventOrder": 2,
      "eventType": "reply",
      "retweetCount": 0,
      "textLen": 45,
      "userProfile": {
        "logFollowers": 5.0,
        "logFollowing": 4.2,
        "logStatuses": 7.8,
        "accountCreatedUnix": 1500000000.0,
        "hasProfile": 0
      },
      "tree": { "depth": 1, "rootOutDegree": 3 },
      "cascade": { "speed": 1.2, "lifespan": 5.0, "burstiness": 0.8 }
    }
  ],
  "includeXai": true
}
```

### 8.2. Raw feature matrix (trước normalize) — ví dụ event #2

Giả sử event #2 (retweet) sau bước log1p/feature engineering:

```text
relative_time_sec   = log1p(300)      ≈ 5.707
delta_t_sec         = log1p(300)      ≈ 5.707
retweet_count       = log1p(12)      ≈ 2.565
event_order_in_article = 1
text_len            = log1p(0)        = 0.0
event_type_idx      = 1               (ví dụ retweet)
u_log_followers     = 10.1
u_log_following     = 7.0
u_log_statuses      = 11.2
u_foll_ratio        = 3.1
u_acct_age_log      = log1p(3650)     ≈ 8.203
u_has_profile       = 1.0
t_depth             = log1p(1)        ≈ 0.693
t_root_outdeg       = log1p(3)        ≈ 1.386
c_speed             = 1.2             (article-level)
c_lifespan          = 5.0
c_burstiness        = 0.8
```

Sau đó:

```python
edge_feats[1] = (raw_vector - EDGE_MEAN) / EDGE_STD
```

---

## 9. Output mẫu — 1 bài viết

```json
{
  "fakeProbability": 0.724,
  "label": "FAKE",
  "riskLevel": "MEDIUM",
  "threshold": 0.59,
  "explanation": "TGNN predicts FAKE with fake probability 0.724 at threshold 0.59. Top propagation signals: retweet at +5m, reply at +15m.",
  "graphContribution": 0.41,
  "eventAttributions": [
    {
      "eventIndex": 1,
      "eventType": "retweet",
      "tigeRemoval": 0.062,
      "confidenceDrop": 0.048,
      "summary": "Retweet at t=+300s increased fake probability"
    },
    {
      "eventIndex": 2,
      "eventType": "reply",
      "tigeRemoval": 0.031,
      "confidenceDrop": 0.019,
      "summary": "Reply at t=+900s increased fake probability"
    }
  ],
  "mode": "full"
}
```

Trường hợp không có interaction (fallback demo):

```json
{
  "fakeProbability": 0.612,
  "label": "FAKE",
  "riskLevel": "MEDIUM",
  "threshold": 0.59,
  "explanation": "TGNN predicts FAKE (degraded text-only fallback — no propagation events).",
  "eventAttributions": [],
  "mode": "text_only_fallback"
}
```

---

## 10. Artifact deploy — checklist

File `tgnn_deployment.pth` cần chứa:

```python
{
    "model_state_dict": ...,
    "cfg": {...},
    "user2idx": {...},
    "event_type2idx": {...},
    "EDGE_FEATURE_NAMES": [...],   # 17 tên, đúng thứ tự
    "EDGE_MEAN": np.ndarray,       # shape [17]
    "EDGE_STD": np.ndarray,        # shape [17]
    "threshold": float,            # best_thr từ validation
    "num_users": int,
}
```

Biến môi trường service:

```env
TGNN_MODEL_PATH=models/tgnn_deployment.pth
TGNN_THRESHOLD=0.59
```

---

## 11. Mapping CyberSocial → features (Phase 2)

### 11.1. Event stream (`PropagationEventBuilder` — backend)

Backend gom timeline từ DB, sort theo `createdAt ASC`:

| Thứ tự | Nguồn | `eventType` | Ghi chú |
|---|---|---|---|
| 1 | `Post` (root) | `tweet` | Event đầu tiên, `textLen = len(content)` |
| … | `PostLike` | `like` | `textLen = 0` |
| … | `PostComment` | `comment` | `textLen = len(comment.content)` |
| … | `PostShare` | `share` | Map ≈ retweet; `textLen` = quote hoặc post gốc |

Mỗi event gửi sang AI service:

- `timestampUnix` — epoch giây
- `eventOrder` — 0..N-1 sau khi sort
- `retweetCount` — số share tích lũy đến thời điểm event (≈ `retweet_count` notebook)
- `srcUserId` — hash ổn định từ UUID user
- `userProfile` — xem mục 11.2
- `tree` — xem mục 11.3

**API phân tích có post:**

```http
POST /api/ai/analyze
{
  "text": "optional fallback",
  "postId": "uuid-cua-post",
  "includeXai": true
}
```

Backend tự build `events` từ DB khi có `postId`. Không có `postId` → `text_only_fallback`.

### 11.2. User profile features

Tính từ `User` + bảng `user_follows` + số bài viết:

| Feature | Công thức CyberSocial |
|---|---|
| `u_log_followers` | `log1p(followerCount)` — `countByFollowingId` |
| `u_log_following` | `log1p(followingCount)` — `countByFollowerId` |
| `u_log_statuses` | `log1p(postCount)` — `countByAuthorId` |
| `u_foll_ratio` | `u_log_followers - u_log_following` |
| `u_acct_age_log` | `log1p((event_unix - user.createdAt) / 86400)` |
| `u_has_profile` | `1.0` nếu có avatar hoặc displayName, else `0.0` |

### 11.3. Tree features (đơn giản hóa)

CyberSocial chưa có cây retweet đầy đủ — dùng heuristic:

| Event | `t_depth` | `t_root_outdeg` |
|---|---|---|
| Root post (`tweet`) | 0 | tổng số share của bài |
| `share` | 1 | tổng số share của bài |
| `like` / `comment` | 0 | tổng số share của bài |

Sau đó `features.py` áp dụng `log1p` trước normalize.

### 11.4. Cascade features

Tính trong `features.py` từ toàn bộ event stream (mục 3.4) — backend **không** gửi `cascade` override.

### 11.5. Feature thiếu (quy tắc thống nhất)

- Profile hoặc tree **không gửi** → raw = `EDGE_MEAN[column]` → sau normalize = **0.0**
- Không random, không bỏ cột
- TIGE mask baseline cũng dùng vector 0 trong không gian đã chuẩn hóa

### 11.6. `event_type_idx`

| CyberSocial | Index mặc định (nếu artifact không có `event_type2idx`) |
|---|---|
| `tweet` | 0 |
| `share` / `retweet` | 1 |
| `comment` / `reply` | 2 |
| `quote` | 3 |
| `like` | 4 |

---

## 12. Tham chiếu code

| Thành phần | Vị trí |
|---|---|
| Feature engineering | `tgnn-tice.ipynb` — section 5b, `prepare_edge_frame` |
| Normalize | `FakeNewsTemporalDataset.__getitem__` |
| Model forward | `class TGNN.forward(input_ids, attention_mask, user_idx, edge_feats)` |
| Threshold tuning | notebook cell — grid `np.arange(0.10, 0.90, 0.01)` |
| Plan triển khai | `../../AI_TGNN_INTEGRATION_PLAN.md` |
