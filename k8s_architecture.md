## Architecture in Kubernetes

```
┌───────────────────────────────────────────────────────────────┐
│   Docker Desktop Kubernetes Cluster                           │
│   Context: docker-desktop                                     │
│                                                               │
│   Namespace: packing-assistant                                │
│   ┌────────────────────────────────────────────────┐          │
│   │  Secret: app-secrets                           │          │
│   │  - postgres-db, postgres-user, postgres-pass   │          │
│   │  - openai-api-key                              │          │
│   └────────────────────────────────────────────────┘          │
│                                                               │
│   ┌─────────────────┐  ┌──────────────────┐                   │
│   │ PostgreSQL      │  │ PVC              │                   │
│   │ Deployment      │←─│ Persistent       │                   │
│   │ Port: 5432      │  │ Storage          │                   │
│   └─────────────────┘  └──────────────────┘                   │
│          ↑                                                    │
│   Service: postgres (ClusterIP)                               │
│          ↑                                                    │
│   ┌──────────────────────────────────────────┐                │
│   │ API Gateway Deployment                   │                │
│   │ Port: 8080                               │                │
│   │ - Session management                     │                │
│   │ - REST API endpoints                     │                │
│   │ - Database persistence                   │                │
│   └──────────────────────────────────────────┘                │
│          ↓                          ↓                         │
│   Service: api-gateway      HTTP → AI Worker                  │
│   (NodePort: 30080)                 Deployment                │
│          ↓                          Port: 8081                │
│          ↓                          - GPT-4 integration       │
│   ┌──────────────────┐              - RAG pipeline            │
│   │ Frontend         │                     ↓                  │
│   │ Deployment       │              Service: ai-worker        │
│   │ Port: 80 (Nginx) │              (ClusterIP)               │
│   └──────────────────┘                     ↓                  │
│          ↓                          Service: qdrant           │
│   Service: frontend                 (ClusterIP)               │
│   (NodePort: 30173)                        ↓                  │
│                              ┌─────────────────────┐          │
│                              │ Qdrant Deployment   │          │
│                              │ Port: 6333          │          │
│                              │ - Vector search     │          │
│                              │ - 140 embeddings    │          │
│                              └─────────────────────┘          │
│                                     ↓                         │
│                              ┌──────────────────┐             │
│                              │ PVC              │             │
│                              │ Vector storage   │             │
│                              └──────────────────┘             │
│                                                               │
└───────────────────────────────────────────────────────────────┘
           ↓                   ↓                    ↓
    localhost:5173      localhost:8080         OpenAI API
    (Frontend UI)       (API Gateway)          (GPT-4 + embeddings)
    (via port-forward)  (via port-forward)
```

---

## RAG System Architecture

The RAG (Retrieval-Augmented Generation) pipeline enhances AI responses:

**5-Stage RAG Pipeline**:
1. **Vector Search**: Converts user query → 1536-dim embedding → searches Qdrant with filters
2. **Contextual Data**: Fetches weather info + culture tips
3. **Enhanced Prompt**: Combines retrieved items + context → sends to GPT-4
4. **GPT Generation**: Creates packing list grounded in expert knowledge
5. **Response Validation**: Validates and returns structured JSON