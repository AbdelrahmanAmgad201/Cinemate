# syntax=docker/dockerfile:1

# ---- stage 1: bake model weights + tokenizer into the image at build time ----
FROM python:3.11-slim AS model-fetch
WORKDIR /fetch
RUN pip install --no-cache-dir huggingface_hub
COPY model/fetch_model.py .
ARG HF_MODEL_REPO
ARG HF_MODEL_REVISION
ENV HF_MODEL_REPO=${HF_MODEL_REPO} \
    HF_MODEL_REVISION=${HF_MODEL_REVISION} \
    MODEL_DIR=/model
RUN python fetch_model.py

# ---- stage 2: runtime (no torch/transformers, just onnxruntime + tokenizers) ----
FROM python:3.11-slim
WORKDIR /app
COPY worker/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY worker/inference.py worker/worker.py ./
COPY --from=model-fetch /model /model
ENV MODEL_DIR=/model
CMD ["python", "worker.py"]
