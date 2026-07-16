import os

import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer

MODEL_DIR = os.environ.get("MODEL_DIR", "/model")
MAX_SEQ_LEN = int(os.environ.get("MAX_SEQ_LEN", "256"))
INTRA_OP_THREADS = int(os.environ.get("ONNX_INTRA_OP_THREADS", "1"))
INTER_OP_THREADS = int(os.environ.get("ONNX_INTER_OP_THREADS", "1"))

LABELS = ("toxic", "severe_toxic")

_tokenizer = Tokenizer.from_file(os.path.join(MODEL_DIR, "tokenizer.json"))
_pad_id = _tokenizer.token_to_id("[PAD]")
_tokenizer.enable_truncation(max_length=MAX_SEQ_LEN)
_tokenizer.enable_padding(pad_id=_pad_id if _pad_id is not None else 0, length=None)  # None = pad to longest in batch

_onnx_file = next(f for f in os.listdir(MODEL_DIR) if f.endswith(".onnx"))
_sess_opts = ort.SessionOptions()
_sess_opts.intra_op_num_threads = INTRA_OP_THREADS
_sess_opts.inter_op_num_threads = INTER_OP_THREADS
_session = ort.InferenceSession(
    os.path.join(MODEL_DIR, _onnx_file),
    sess_options=_sess_opts,
    providers=["CPUExecutionProvider"],
)
_input_names = {i.name for i in _session.get_inputs()}


def _sigmoid(x: np.ndarray) -> np.ndarray:
    return 1.0 / (1.0 + np.exp(-x))


def score(texts: list[str]) -> list[dict[str, float]]:
    """Order-preserving. Internally sorts by length (length bucketing) so padding
    within the batch is minimized, then restores the caller's order."""
    if not texts:
        return []

    order = sorted(range(len(texts)), key=lambda i: len(texts[i]))
    encodings = _tokenizer.encode_batch([texts[i] for i in order])

    feed = {}
    if "input_ids" in _input_names:
        feed["input_ids"] = np.array([e.ids for e in encodings], dtype=np.int64)
    if "attention_mask" in _input_names:
        feed["attention_mask"] = np.array([e.attention_mask for e in encodings], dtype=np.int64)
    if "token_type_ids" in _input_names:
        feed["token_type_ids"] = np.array([e.type_ids for e in encodings], dtype=np.int64)

    (logits,) = _session.run(None, feed)
    probs = _sigmoid(logits)

    sorted_results = [{LABELS[0]: float(row[0]), LABELS[1]: float(row[1])} for row in probs]
    results: list[dict[str, float] | None] = [None] * len(texts)
    for original_idx, result in zip(order, sorted_results):
        results[original_idx] = result
    return results  # type: ignore[return-value]
