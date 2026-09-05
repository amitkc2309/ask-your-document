# Ask Your Document

Ask Your Document is a sophisticated document management and intelligent search system that leverages AI to provide precise answers based on your uploaded documents. It features a modern microservices-friendly architecture with support for both traditional keyword search and advanced Retrieval-Augmented Generation (RAG).

## 🚀 Features

- **Multi-format Document Support**: Upload and process PDF, DOCX, TXT, and RTF files.
- **Hybrid Search**: Combines traditional BM25 keyword search with Vector KNN search for optimal retrieval.
- **AI-Powered Answers (RAG)**: Uses Large Language Models (LLM) to answer questions based on your documents with streaming responses (SSE).
- **Asynchronous Processing**: Scalable document ingestion using Kafka for text extraction and embedding generation.
- **Fine-grained Security**: Robust authentication and authorization powered by Keycloak, including path-based policy enforcement.
- **Modern UI**: Clean and responsive React-based frontend for document management and interactive search.
- **Dual Mode**: Switch between AI-enhanced search and standard Elasticsearch-based search.

## 🏗️ Technical Architecture

The project follows a distributed architecture with the following components:

- **Frontend**: React-based Single Page Application.
- **Backend**: Spring Boot 3 application with Virtual Threads enabled.
- **Storage**: 
  - **PostgreSQL**: Stores document metadata and application state.
  - **MinIO**: Object storage for original document files.
  - **Elasticsearch**: Acts as both a full-text search engine and a Vector database for embeddings.
- **Messaging**: **Apache Kafka** handles asynchronous document processing tasks.
- **Security**: **Keycloak** manages users, tokens (JWT), and fine-grained access policies.
- **Cache**: **Redis** is used for caching security policies and other transient data.

### Document Ingestion Flow
1. User uploads a file via the API.
2. Metadata is saved to PostgreSQL; file is stored in MinIO.
3. An upload event is pushed to Kafka.
4. The Document Processor (Kafka Consumer) picks up the event.
5. Text is extracted, chunked, and embedded using an Embedding Model.
6. Chunks and their vectors are indexed in Elasticsearch.

### Search Flow (AI Mode)
1. User submits a natural language query.
2. Query is optionally transformed/optimized by an LLM.
3. **Hybrid Search**: Simultaneous BM25 and Vector KNN search in Elasticsearch.
4. **Reciprocal Rank Fusion (RRF)**: Merges results from both search methods.
5. **Re-ranking**: Top results are re-evaluated for relevance.
6. **LLM Generation**: The top context chunks are sent to the LLM (via Spring AI) to generate a final answer.
7. The answer is streamed back to the user via Server-Sent Events (SSE).

## 🔒 Security Configuration

Security is a core pillar of "Ask My Doc", implemented using **Spring Security** and **Keycloak**:

- **Authentication**: JWT-based authentication. The frontend obtains a token from Keycloak and includes it in the `Authorization` header.
- **Authorization**: 
  - **Keycloak Policy Enforcer**: A custom filter (`KeycloakAuthFilter`) intercepts requests and validates them against Keycloak's Authorization Services (UMA).
  - **Resource-Based Access**: Every API request is checked for specific permissions (Scopes like GET, POST, DELETE) against the resource (e.g., `/api/documents`).
  - **Data Isolation**: All database queries (Postgres and Elasticsearch) are filtered by the `uploadedBy` field (extracted from the JWT) to ensure users only see their own documents.

## 🛠️ Tech Stack

- **Backend**: Java 21, Spring Boot 3.4+, Spring AI, Spring Data JPA, Spring Data Elasticsearch, Spring Kafka.
- **Frontend**: React, TypeScript, Tailwind CSS, Vite.
- **AI/ML**: Spring AI (supporting various LLM providers), Elasticsearch Vector Search, Re-ranking services.
- **Infrastructure**: Docker & Docker Compose, PostgreSQL, Elasticsearch, Kafka, MinIO, Keycloak, Redis.

## 🚦 Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 21+ (for backend development)
- Node.js & npm (for frontend development)

### Quick Start with Docker
The project includes a `ask-my-doc-docker` directory with a pre-configured `docker-compose.yml` to spin up all infrastructure services.

1. Start infrastructure:
   ```bash
   cd ask-my-doc-docker
   docker-compose up -d
   ```
2. Configure Keycloak:
   - Access Keycloak at `http://localhost:7080`.
   - Create a realm `ask-my-doc` and a client `ask-my-doc-backend`.
   - Setup Authorization services and policies (refer to `note.md` for specific details).

3. Run the Backend:
   ```bash
   cd ask-my-doc-backend/amd-manage-doc
   ./mvnw spring-boot:run
   ```

4. Run the Frontend:
   ```bash
   cd ask-my-doc-frontend/ask-my-doc
   npm install
   npm run dev
   ```

## ⚙️ Configuration

Key configuration properties in `application.yaml`:
- `application.ai-mode`: Toggle between `true` (RAG) and `false` (Standard Search).
- `spring.ai.openai.api-key`: Your OpenAI API key (if using OpenAI).
- `application.keycloak.*`: Connection details for Keycloak.
- `spring.elasticsearch.uris`: Elasticsearch connection string.

