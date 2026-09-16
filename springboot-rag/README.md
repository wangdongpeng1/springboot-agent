**参考来源** https://chatgpt.com/c/6a2ff695-3460-83ec-89f3-0f7077cb0522

**系统架构**
```
springboot-rag
│
├── config
│   ├── ChatClientConfig.java
│   ├── MemoryConfig.java
│   ├── RagAdvisorConfig.java
│   └── GraphRagAdvisorConfig.java
│
├── rag
│   ├── transformer
│   │   ├── RewriteConfig.java
│   │   ├── CompressionConfig.java
│   │   └── TranslationConfig.java
│   │
│   ├── expander
│   │   └── MultiQueryExpanderConfig.java
│   │
│   ├── retriever
│   │   └── HybridRetrieverConfig.java
│   │
│   ├── service
│   │   └── RagService.java
│
└── controller
    └── ChatController.java
```

**企业级 RAG 架构**
```
                ┌──────────────┐
                │   Query      │
                └─────┬────────┘
                      ↓
            ┌──────────────────┐
            │ Query Rewrite    │  ✔ ChatClient
            └──────────────────┘
                      ↓
            ┌──────────────────┐
            │ Multi Query      │  ✔ ChatClient Prompt
            └──────────────────┘
                      ↓
        ┌──────────────┬──────────────┐
        ↓                              ↓
 Milvus Vector Search        Neo4j Graph Query (Cypher)
        ↓                              ↓
        └──────────────┬──────────────┘
                       ↓
            DocumentJoiner (merge)
                       ↓
            🔥 Reranker（BGE / Jina）
                       ↓
                 TopK Filter
                       ↓
            Context Builder
                       ↓
                 LLM (Ollama)
                       ↓
                   Answer
```
**LangGraph4j**
- 官方文档 https://langgraph4j.github.io/langgraph4j/
- 浏览器访问 http://localhost:<port>/?instance=<instance_id>

#### Agentic Workflow

```
                    ┌──────────────┐
                    │    START     │
                    └──────┬───────┘
                           ↓
                    ┌──────────────┐
                    │    rewrite   │
                    └──────┬───────┘
                           ↓
                    ┌──────────────┐
                    │   classify   │
                    └──────┬───────┘
                           │
                  ┌────────┴────────┐
                  │                 │
               NORMAL              RAG
                  │                 │
                  ↓                 ↓
              ┌───────┐       ┌──────────┐
              │normal │       │   rag    │
              └───┬───┘       └────┬─────┘
                  │                 │
                  │                 ↓
                  │            ┌─────────┐
                  │            │ answer  │
                  │            └────┬────┘
                  │                 │
                  └────────┬────────┘
                           ↓
                         END
```
#### Agentic Workflow Upgrade (AI Workflow Orchestrator)

```
                    ┌───────────────────────┐
                    │      User Query       │
                    └───────────┬───────────┘
                                ↓
                       ┌────────────────┐
                       │ Rewrite Query  │
                       └───────┬────────┘
                               ↓
                       ┌────────────────┐
                       │ Multi Query    │
                       │ Expansion      │
                       └───────┬────────┘
                               ↓
                     ┌────────────────────┐
                     │   Intent Router    │
                     └─────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ↓                ↓                ↓
          Normal             RAG             Agent
              │                │                │
              │       ┌────────┴───────┐        │
              │       ↓                ↓        │
              │    Milvus            Neo4j      │
              │       │                │        │
              │       └───────┬────────┘        │
              │               ↓                 │
              │          DocumentJoin            │
              │               ↓                 │
              │             Rerank               │
              │               ↓                 │
              └───────────────┼─────────────────┘
                              ↓
                          LLM Answer
                              ↓
                         Chat Memory
                              ↓
                             END
```
