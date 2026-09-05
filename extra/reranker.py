from fastapi import FastAPI
from pydantic import BaseModel
import torch
import os
from sentence_transformers import CrossEncoder

app = FastAPI()

# -----------------------------
# Device selection
# -----------------------------
# values:
# auto | cpu | cuda
DEVICE_MODE = os.getenv("DEVICE", "auto").lower()

if DEVICE_MODE == "cpu":
    device = "cpu"

elif DEVICE_MODE == "cuda":
    if not torch.cuda.is_available():
        print("CUDA requested but GPU unavailable; selecting CPU")
        device = "cpu"
    else:
        device = "cuda"

else:
    device = "cuda" if torch.cuda.is_available() else "cpu"

print("CUDA available:", torch.cuda.is_available())
print("Selected device:", device)

if torch.cuda.is_available():
    print("GPU:", torch.cuda.get_device_name(0))


# -----------------------------
# Model
# -----------------------------
#model_name = "cross-encoder/ms-marco-MiniLM-L-12-v2"
model_name = "BAAI/bge-reranker-v2-m3"
#model_name = "BAAI/bge-reranker-base"

model = CrossEncoder(
    model_name,
    device=device
)
print(f"Model loaded on: {device}")

# -----------------------------
# Request models
# -----------------------------

class RerankItem(BaseModel):
    id: str
    text: str


class RerankRequest(BaseModel):
    query: str
    documents: list[RerankItem]

# -----------------------------
# Health endpoint
# -----------------------------
@app.get("/health")
def health():
    return {
        "status": "UP",
        "device": device,
        "cuda_available": torch.cuda.is_available()
    }

# -----------------------------
# Rerank endpoint
# -----------------------------

@app.post("/rerank")
def rerank(req: RerankRequest):
    # create (query, text) pairs
    pairs = [(req.query, doc.text) for doc in req.documents]

    scores = model.predict(pairs)

    results = []
    for doc, score in zip(req.documents, scores):
        results.append({
            "id": doc.id,       # return SAME id
            "score": float(score)
        })

    # sort descending
    results.sort(key=lambda x: x["score"], reverse=True)

    return results