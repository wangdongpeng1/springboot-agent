# Docker 构建、导出、运行指南

## 前置准备

### Maven 打包（在根目录执行）

```bash
mvn clean package -DskipTests
```

### 创建部署目录并复制产物

```bash
mkdir -p /home/dockerBuild/springboot-agent
cp springboot-gateway/target/springboot-gateway-0.0.1-SNAPSHOT.jar /home/dockerBuild/springboot-agent/
cp springboot-rag/target/springboot-rag-0.0.1-SNAPSHOT.jar       /home/dockerBuild/springboot-agent/
cp springboot-sentinel/target/springboot-sentinel-0.0.1-SNAPSHOT.jar /home/dockerBuild/springboot-agent/
cp springboot-mcp-sse/target/springboot-mcp-sse-0.0.1-SNAPSHOT.jar /home/dockerBuild/springboot-agent/
cp springboot-mcp-stream/target/springboot-mcp-stream-0.0.1-SNAPSHOT.jar /home/dockerBuild/springboot-agent/
cp springboot-ocr/target/springboot-ocr-0.0.1-SNAPSHOT.jar       /home/dockerBuild/springboot-agent/
cp dockerBuild/logback.xml /home/dockerBuild/
cp dockerBuild/Dockerfile-* /home/dockerBuild/
```

---

## 构建镜像

### 进入工作目录

```bash
cd /home/dockerBuild
```

### 构建 gateway 镜像（API 网关）

```bash
docker build -t springboot-agent-gateway:test -f Dockerfile-gateway .
```

### 构建 rag 镜像（RAG 增强生成）

```bash
docker build -t springboot-agent-rag:test -f Dockerfile-rag .
```

### 构建 sentinel 镜像（流量治理）

```bash
docker build -t springboot-agent-sentinel:test -f Dockerfile-sentinel .
```

### 构建 mcp-sse 镜像（MCP SSE 通信）

```bash
docker build -t springboot-agent-mcp-sse:test -f Dockerfile-sse .
```

### 构建 mcp-stream 镜像（MCP 流式处理）

```bash
docker build -t springboot-agent-mcp-stream:test -f Dockerfile-stream .
```

### 构建 ocr 镜像（光学字符识别）

```bash
docker build -t springboot-agent-ocr:test -f Dockerfile-ocr .
```

### 查看构建好的镜像

```bash
docker images | grep springboot-agent
```

---

## 导出镜像

```bash
docker save -o springboot-agent-gateway-test.tar  springboot-agent-gateway:test
docker save -o springboot-agent-rag-test.tar      springboot-agent-rag:test
docker save -o springboot-agent-sentinel-test.tar  springboot-agent-sentinel:test
docker save -o springboot-agent-mcp-sse-test.tar   springboot-agent-mcp-sse:test
docker save -o springboot-agent-mcp-stream-test.tar springboot-agent-mcp-stream:test
docker save -o springboot-agent-ocr-test.tar       springboot-agent-ocr:test
```

### 查看导出的文件

```bash
ls -lh *.tar
```

---

## 运行镜像

### 运行 gateway 镜像

```bash
docker run -d \
  --name springboot-agent-gateway \
  --restart unless-stopped \
  -p 9800:9800 \
  -v agent-gateway-logs:/agent/logs \
  springboot-agent-gateway:test
```

### 运行 rag 镜像

```bash
docker run -d \
  --name springboot-agent-rag \
  --restart unless-stopped \
  -p 8080:8080 \
  -v agent-rag-logs:/agent/logs \
  springboot-agent-rag:test
```

### 运行 sentinel 镜像

```bash
docker run -d \
  --name springboot-agent-sentinel \
  --restart unless-stopped \
  -p 8090:8090 \
  -v agent-sentinel-logs:/agent/logs \
  springboot-agent-sentinel:test
```

### 运行 mcp-sse 镜像

```bash
docker run -d \
  --name springboot-agent-mcp-sse \
  --restart unless-stopped \
  -p 9801:9801 \
  -v agent-mcp-sse-logs:/agent/logs \
  springboot-agent-mcp-sse:test
```

### 运行 mcp-stream 镜像

```bash
docker run -d \
  --name springboot-agent-mcp-stream \
  --restart unless-stopped \
  -p 9802:9802 \
  -v agent-mcp-stream-logs:/agent/logs \
  springboot-agent-mcp-stream:test
```

### 运行 ocr 镜像

```bash
docker run -d \
  --name springboot-agent-ocr \
  --restart unless-stopped \
  -p 8081:8081 \
  -v agent-ocr-logs:/agent/logs \
  springboot-agent-ocr:test
```
