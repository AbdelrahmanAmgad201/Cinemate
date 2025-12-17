from fastapi import FastAPI, APIRouter
from pydantic import BaseModel
import nltk
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch

# Download NLTK punkt tokenizer
nltk.download('punkt')
nltk.download('punkt_tab')

# Load model and tokenizer (replace with your model path)
model_name = "facebook/roberta-hate-speech-dynabench-r4-target"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSequenceClassification.from_pretrained(model_name)

labels = ["Not hate", "Hate"]

app = FastAPI()
api_router = APIRouter(prefix="/api/hate/v1")

class TextRequest(BaseModel):
    text: str

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

@api_router.post("/analyze")
def analyze(request: TextRequest):
    print(request.text)
    return analyze_text(request.text)
@api_router.get("/health")
def health():
    return {"status": "ok", "message": "FastAPI server is running"}

app.include_router(api_router)