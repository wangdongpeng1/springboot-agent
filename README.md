# SpringBoot AI Agent Platform

基于 Spring Boot 4.1 + Spring AI 2.0 构建的 AI 代理服务平台，采用多模块微服务架构，集成 RAG 增强生成、MCP 协议通信、流量治理、OCR 图文分析等核心能力。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 | LTS 长期支持版本 |
| Spring Boot | 4.1.0 | 核心框架 |
| Spring AI | 2.0.0 | AI 能力集成 |
| Spring Cloud | 2025.0.1 | 微服务治理 |
| Spring Cloud Alibaba | 2025.0.0.0 | Sentinel 流量治理 |
| Ollama | - | 本地大模型推理（qwen2.5:7b） |
| Milvus | - | 向量数据库 |
| Neo4j | - | 图数据库 |
| Redis | - | 缓存 & 会话记忆 |
| MinIO | - | 对象存储 |
| Kafka | - | 异步消息队列 |
| Docker | eclipse-temurin:21 | 容器化部署 |

## 项目结构

```
springboot-agent/
├── springboot-gateway/        # API 网关
├── springboot-rag/            # RAG 增强生成
├── springboot-sentinel/       # 流量治理
├── springboot-kafka/          # Kafka 异步任务队列
├── springboot-mcp-host/       # MCP 客户端（工具库）
├── springboot-mcp-sse/        # MCP Server（SSE 协议）
├── springboot-mcp-stream/     # MCP Server（Streamable-HTTP 协议）
├── springboot-ocr/            # 图文分析 & OCR
├── dockerBuild/               # Docker 构建配置
└── wiki/                      # 项目文档
```

## 模块说明

### springboot-gateway — API 网关

基于 **Spring Cloud Gateway** 实现的统一 API 网关，负责请求路由、鉴权过滤和流量转发。

- 端口：`9800`
- 核心能力：路由分发（SSE/Stream MCP 协议）、请求鉴权、跨域处理
- 路由规则将 MCP SSE 请求转发至 `springboot-mcp-sse`，Streamable-HTTP 请求转发至 `springboot-mcp-stream`

### springboot-rag — RAG 增强生成

基于 **Spring AI** 实现的检索增强生成（Retrieval-Augmented Generation）服务。

- 端口：`8080`
- 核心能力：文档向量化、知识库问答、对话记忆、知识图谱
- 技术组件：
  - **Ollama**：本地大模型推理（对话模型 qwen2.5:7b，嵌入模型 nomic-embed-text）
  - **Milvus**：向量数据库，存储文档嵌入向量
  - **Neo4j**：图数据库，构建知识图谱
  - **Redis**：对话记忆缓存
- 附带 Web UI（`rag-ui/`）：基于 React + TypeScript 的前端界面，支持知识图谱可视化和对话交互

### springboot-sentinel — 流量治理

基于 **Spring Cloud Alibaba Sentinel** 实现的流量治理服务。

- 端口：`8090`
- 核心能力：流量控制、熔断降级、系统负载保护
- 内置 Sentinel Dashboard（`sentinel-dashboard-1.8.9.jar`）用于可视化监控

### springboot-kafka — Kafka 异步任务队列

基于 **Spring Kafka** 实现的异步任务生产与消费示例服务，用于演示任务提交、顺序消费和手动提交 Offset。

- 端口：`9090`
- Kafka 地址：`localhost:9092`
- Topic：`task-topic`，默认 `1` 个分区、`1` 个副本，用于保证单机环境下全局顺序处理
- 消费组：`task-group`
- REST 接口：
  - `POST /api/tasks`：提交单个任务，Body 示例：`"hello task"`
  - `POST /api/tasks/batch`：批量提交任务，Body 示例：`["task1", "task2", "task3"]`
- 消费策略：`concurrency: 1`、`max-poll-records: 1`、`ack-mode: manual`，任务处理成功后才手动提交 Offset，失败时保留消息用于重试

### springboot-mcp-host — MCP 客户端

**Spring AI MCP Client** 封装模块，作为共享工具库被其他服务引用（非独立运行）。

- 核心能力：MCP 客户端连接管理、工具调用封装、DTO 数据传输
- 提供 MCP Server 配置管理（`McpServerConfig`）和工具注册机制

### springboot-mcp-sse — MCP Server（SSE 协议）

基于 **Spring AI MCP Server** 实现的 SSE（Server-Sent Events）协议服务端。

- 端口：`9801`
- SSE 端点：`/api/v1/sse`
- 消息端点：`/api/v1/mcp/messages`
- 核心能力：SSE 长连接通信、MCP 工具注册与调用、异步消息推送

### springboot-mcp-stream — MCP Server（Streamable-HTTP 协议）

基于 **Spring AI MCP Server** 实现的 Streamable-HTTP 协议服务端。

- 端口：`9802`
- MCP 端点：`/api/v1/mcp`
- 核心能力：Streamable-HTTP 流式通信、MCP 工具注册与调用、双向数据流

### springboot-ocr — 图文分析 & OCR

集成 **MinerU**、**Chinese-CLIP** 和 **MinIO** 的图文分析服务。

- 端口：`8081`
- 核心能力：
  - **MinerU**：PDF 文档解析与 OCR 识别
  - **Chinese-CLIP**：图文相似度匹配分析
  - **MinIO**：对象存储（文件上传/下载/预签名链接/Base64 编码）
- 工作流程：PDF → 图片转换 → MinerU OCR → Chinese-CLIP 图文匹配 → 结果存储

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- Docker（可选，容器化部署）
- Ollama（RAG 模块依赖）
- Milvus（RAG 模块依赖）
- Redis（RAG 模块依赖）
- MinIO（OCR 模块依赖）
- Kafka（Kafka 模块依赖）

### 编译打包

```bash
# 根目录执行
mvn clean package -DskipTests
```

### 本地运行

```bash
# 以 Gateway 为例
cd springboot-gateway
mvn spring-boot:run

# 或以 JAR 方式运行
java -jar springboot-gateway/target/springboot-gateway-0.0.1-SNAPSHOT.jar
```

### Docker 部署

详细步骤参见 [dockerBuild/README.md](dockerBuild/README.md)

```bash
# 快速构建单个镜像示例
cd dockerBuild
docker build -t springboot-agent-gateway:test -f Dockerfile-gateway .

# 运行
docker run -d \
  --name springboot-agent-gateway \
  --restart unless-stopped \
  -p 9800:9800 \
  -v agent-gateway-logs:/agent/logs \
  springboot-agent-gateway:test
```

## 配置说明

各模块支持多环境配置（dev/test/pro），通过 `spring.profiles.active` 切换：

| 环境 | 配置文件 | 用途 |
|------|----------|------|
| dev | `application-dev.yml` | 本地开发 |
| test | `application-test.yml` | 测试环境 |
| pro | `application-pro.yml` | 生产环境 |

## 编码规范（AI 协作必读）

> **本项目遵循企业级 Java 编码规范，所有代码（包括 AI 生成）必须严格遵守以下准则。**
> 详细规范参见 [wiki/CODING_PROMPT.md](wiki/CODING_PROMPT.md)
> 10年Java匠心沉淀 · 106行完美主义编码规范

### 开发前必须检查

- **版本兼容**：所有代码必须严格基于当前项目的 JDK 21 / Spring Boot 4.1.0 版本开发，禁止使用当前环境不支持的 API、语法或特性
- **依赖管控**：新增 Maven 依赖必须确认与当前版本栈兼容，优先选择官方维护中的稳定版本，不盲目追求最新

### 编码风格

**业务代码**：优先采用现代 Java 风格 —— Stream API、Lambda、方法引用、Optional、函数式接口，追求简洁、可读、类型安全

**框架/基础能力代码**（Starter、SDK、中间件、底层封装）：优先保证兼容性、稳定性、性能和可调试性，避免过度函数式抽象

> 核心原则：**不为风格而风格** —— 当 Stream 链式操作导致嵌套过深或调试困难时，应选择清晰易维护的传统写法

### 代码设计

- 遵循 **SOLID** 和 **DRY** 原则
- 避免重复代码和过度设计
- 编码前先分析业务目标、数据流转、类职责和扩展性
- 优先考虑可维护性、扩展性和可测试性

### 代码输出标准

- 必须提供**生产级实现**，不输出简单 Demo 级代码
- 不使用不存在或版本不支持的 API
- 完成后自动检查版本兼容性、设计合理性和重复逻辑

---

## License

MIT
