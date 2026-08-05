# TGNN Model Artifacts

Đặt checkpoint huấn luyện từ notebook tại:

```text
models/tgnn_tice.pt
```

Service đọc mặc định từ `TGNN_MODEL_PATH=models/tgnn_tice.pt`.

## Cấu trúc checkpoint hỗ trợ (`tgnn_tice.pt`)

Export từ notebook (`best_clean_tgnn_improved.pt` / tương đương) với các key:

| Key | Bắt buộc | Mô tả |
|---|---|---|
| `model_state_dict` | ✅ | Weights TGNN |
| `CFG` hoặc `cfg` | ✅ | Hyperparams (`hidden_dim`, `edge_feat_dim`, …) |
| `EDGE_MEAN` / `EDGE_STD` | ✅ | Z-score 17 edge features |
| `EDGE_FEATURE_NAMES` | ✅ | 17 tên feature đúng thứ tự MODEL_SPEC |
| `event_type2idx` | ✅ | Thường `{reply:0, retweet:1, share:2}` |
| `user2idx` | khuyến nghị | Map user → index |
| `threshold` / `best_thr` | tùy chọn | Nếu thiếu → dùng `TGNN_THRESHOLD` (mặc định 0.5) |
| `tige_temperature` / `T_robust` | tùy chọn | Temperature cho TIGE |

Không commit file `.pt` lớn lên Git trừ khi repo dùng Git LFS.
