# -------------------------
# Builder stage: download and build packages
# -------------------------
FROM python:3.10-slim AS builder

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# Install essential build tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir \
    torch --index-url https://download.pytorch.org/whl/cpu && \
    pip install --no-cache-dir \
    nltk fastapi uvicorn transformers

# -------------------------
# Download models and NLTK data
# -------------------------
RUN mkdir -p /models /nltk_data && \
    python - <<EOF
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import nltk, os

# Transformers model
model_name = "facebook/roberta-hate-speech-dynabench-r4-target"
AutoTokenizer.from_pretrained(model_name, cache_dir="/models")
AutoModelForSequenceClassification.from_pretrained(model_name, cache_dir="/models")

# NLTK data
nltk.download('punkt', download_dir='/nltk_data')
nltk.download('punkt_tab', download_dir='/nltk_data')
EOF

# -------------------------
# Final runtime stage
# -------------------------
FROM python:3.10-slim

ENV PYTHONUNBUFFERED=1 \
    TRANSFORMERS_CACHE=/models \
    TRANSFORMERS_OFFLINE=1 \
    NLTK_DATA=/nltk_data

WORKDIR /app

# Install minimal tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -m -u 1001 appuser

# Copy installed packages from builder
COPY --from=builder /usr/local/lib/python3.10/site-packages /usr/local/lib/python3.10/site-packages
COPY --from=builder /usr/local/bin/uvicorn /usr/local/bin/uvicorn

# Copy models and NLTK data
COPY --from=builder /models /models
COPY --from=builder /nltk_data /nltk_data

# Copy app
COPY app.py .

# Set permissions
RUN chown -R appuser:appuser /app /models /nltk_data

USER appuser

EXPOSE 8000

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8000/api/hate/v1/health || exit 1

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
