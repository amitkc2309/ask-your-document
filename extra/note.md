Encoder: ollama pull nomic-embed-text

http://localhost:6333/dashboard --> Qdrant
# Example Curl to create a Hybrid-ready collection --> tell Qdrant to enable keyword search as well-
curl -X PUT http://localhost:6333/collections/ask_my_doc_vectors \
-H 'Content-Type: application/json' \
-d '{
"vectors": { "size": 768, "distance": "Cosine" },
"sparse_vectors": { "text-sparse": { "index": { "on_disk": false } } }
}'

Reranker:
pip install fastapi uvicorn sentence-transformers
python -m pip install sentence-transformers
run reranker (run in gitbash)-
python -m uvicorn reranker:app --reload --port 8001
of force DEVICE
DEVICE=cuda python -m uvicorn reranker:app --reload --port 8001

test reranker-
curl -X POST http://127.0.0.1:8001/rerank \
-H "Content-Type: application/json" \
-d '{
"query": "How to configure OAuth in Spring Boot",
"documents": [
{"id": "1", "text": "Spring Security OAuth2 setup guide"},
{"id": "2", "text": "Kafka consumer configuration"},
{"id": "3", "text": "OAuth2 login flow in Spring Boot"}
]
}'

-----
pip uninstall torch -y
pip install torch --index-url https://download.pytorch.org/whl/cu12
--------
# AI mode
docker compose --profile ai up -d

# NON-AI mode
docker compose --profile non-ai up -d

mvn spring-boot:run -Dspring-boot.run.profiles=ai
java -jar app.jar --spring.profiles.active=ai

SPRING_PROFILES_ACTIVE=ai
-----
Embedding dimension at create Index time: 768
-----