# TGNN Model Artifacts

Place the exported TGNN artifact here:

```text
models/tgnn_artifact.pt

```

The FastAPI service expects a PyTorch artifact with:

- `state_dict` or `model_state_dict`: trained `FakeNewsTGNN` weights
- `num_nodes`: number of user/article node ids used by the memory bank
- `threshold`: validation-selected fake probability threshold
- `normalization.means`: mean values for `delta_t`, `event_order`, `retweet_count`, `favorite_count`
- `normalization.stds`: std values for the same features
- `model_name`: BERT model name, usually `bert-base-uncased`
- `max_len`: tokenizer max length, usually `256`
- `config`: optional TGNN hyperparameters from training

Do not commit large `.pt` model files to Git unless the repository is configured for Git LFS.
