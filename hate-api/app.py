import logging
import os
import secrets

from fastapi import Depends, FastAPI, APIRouter, Header, HTTPException
from pydantic import BaseModel, Field
import nltk
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

INTERNAL_API_KEY = os.environ.get("HATE_API_KEY", "")

# Max input length (HS-05d) — without this, a multi-MB string runs NLTK tokenization
# plus one model inference per sentence, with no timeout, making the service trivially
# denial-of-serviceable.
MAX_TEXT_LENGTH = 10_000

# NLTK data is already pre-downloaded to NLTK_DATA at image build time (see
# hate-api/Dockerfile) — nltk.download() here was a redundant network call on every
# cold start that could only ever be a no-op in the container (HS-05b).

# Load model and tokenizer (replace with your model path)
model_name = "facebook/roberta-hate-speech-dynabench-r4-target"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSequenceClassification.from_pretrained(model_name)

labels = ["Not hate", "Hate"]

app = FastAPI()
api_router = APIRouter(prefix="/api/hate/v1")

class TextRequest(BaseModel):
    text: str = Field(..., max_length=MAX_TEXT_LENGTH)

def verify_internal_api_key(x_internal_api_key: str = Header(default="")):
    if not INTERNAL_API_KEY or not secrets.compare_digest(x_internal_api_key, INTERNAL_API_KEY):
        raise HTTPException(status_code=401, detail="Unauthorized")

def classify_sentence(sentence):
    inputs = tokenizer(sentence, return_tensors="pt", truncation=True)
    with torch.no_grad():
        logits = model(**inputs).logits
    probs = torch.softmax(logits, dim=1)[0]
    idx = torch.argmax(probs).item()
    return labels[idx], float(probs[idx])

def analyze_text(text):
    sentences = nltk.sent_tokenize(text)
    for s in sentences:
        label, prob = classify_sentence(s)
        if label == labels[1]:
            return False

    return True

# One-time warmup inference (HS-05c) so the first real request isn't slowed by
# PyTorch's lazy JIT compilation/caching — the Dockerfile healthcheck only hits
# /health, which doesn't run inference, so without this the container was declared
# healthy before it had actually processed a single real request.
try:
    analyze_text("This is a warmup request.")
    logger.info("Model warmup inference completed successfully")
except Exception:
    logger.exception("Model warmup inference failed")

@api_router.post("/analyze", dependencies=[Depends(verify_internal_api_key)])
def analyze(request: TextRequest):
    return analyze_text(request.text)
@api_router.get("/health")
def health():
    return {"status": "ok", "message": "FastAPI server is running"}

app.include_router(api_router)