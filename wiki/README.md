# 中间件部署指南

## Milvus
官网: https://milvus.io/zh

### 在 Docker 中运行 Milvus（Windows）

#### 方式一：手动部署（推荐，支持数据持久化）

1. 创建配置文件
```powershell
New-Item -ItemType Directory -Path "D:\Milvus" -Force

# 创建 etcd 配置文件
@"
listen-client-urls: http://0.0.0.0:2379
advertise-client-urls: http://0.0.0.0:2379
quota-backend-bytes: 4294967296
auto-compaction-mode: revision
auto-compaction-retention: '1000'
"@ | Out-File -FilePath "D:\Milvus\embedEtcd.yaml" -Encoding utf8 -NoNewline

# 创建用户配置文件
@"
# Extra config to override default milvus.yaml
"@ | Out-File -FilePath "D:\Milvus\user.yaml" -Encoding utf8 -NoNewline
```

2. 创建 Docker named volume（数据存储在 WSL2 ext4 文件系统，性能接近原生）
```powershell
docker volume create milvus-data
```

3. 启动容器
```powershell
docker run -d --name milvus-standalone `
  --security-opt seccomp:unconfined `
  -e ETCD_USE_EMBED=true `
  -e ETCD_DATA_DIR=/var/lib/milvus/etcd `
  -e ETCD_CONFIG_PATH=/milvus/configs/embedEtcd.yaml `
  -e COMMON_STORAGETYPE=local `
  -e DEPLOY_MODE=STANDALONE `
  -v milvus-data:/var/lib/milvus `
  -v "D:\Milvus\embedEtcd.yaml:/milvus/configs/embedEtcd.yaml" `
  -v "D:\Milvus\user.yaml:/milvus/configs/user.yaml" `
  -p 19530:19530 -p 9091:9091 -p 2379:2379 `
  --health-cmd="curl -f http://localhost:9091/healthz" `
  --health-interval=30s `
  --health-start-period=90s `
  --health-timeout=20s `
  milvusdb/milvus:v3.0-beta milvus run standalone
```

3. 管理命令
```powershell
# 查看状态
docker ps --filter "name=milvus"

# 停止
docker stop milvus-standalone

# 启动
docker start milvus-standalone

# 查看日志
docker logs milvus-standalone --tail 50
```

**重要说明：**
- `ETCD_USE_EMBED=true` 必须设置，否则 Milvus 无法启动内嵌 etcd
- 数据持久化使用 Docker named volume `milvus-data`（存储在 WSL2 ext4 文件系统），**不要用 Windows 目录 bind mount**（WSL2 跨文件系统 I/O 极慢，会导致 etcd 超时和 collection 恢复失败）

#### 方式二：使用官方脚本

#### 前提条件
  * 安装 Docker Desktop。
  * 安装 Windows Subsystem for Linux 2 (WSL 2)。
  * 安装 Python 3.8+。
#### 从 PowerShell 或 Windows 命令提示符
1. 在管理员模式下右击并选择以管理员身份运行，打开 Docker Desktop。
2. 下载安装脚本并将其保存为`standalone.bat`。
    ```
    D:\Milvus\>Invoke-WebRequest https://raw.githubusercontent.com/milvus-io/milvus/refs/heads/master/scripts/standalone_embed.bat -OutFile standalone.bat
    ```
3. 运行下载的脚本，将 Milvus 作为 Docker 容器启动。
   ```
   D:\Milvus\>standalone.bat start
   Wait for Milvus starting...
   Start successfully.
   To change the default Milvus configuration, edit user.yaml and restart the service.
   ```
   运行安装脚本后
   * 名为Milvus-standalone的 docker 容器已在19530 端口启动。
   * 嵌入式 etcd 与 Milvus 安装在同一个容器中，服务端口为2379。其配置文件被映射到当前文件夹中的embedEtcd.yaml。
   * Milvus 数据卷映射到当前文件夹中的volumes/milvus。
   
   可以使用以下命令管理 Milvus 容器和存储的数据。
   ```
   # Stop Milvus
   D:\Milvus\>standalone.bat stop
   Stop successfully.
   # Delete Milvus container
   D:\Milvus\>standalone.bat delete
   Delete Milvus container successfully. # Container has been removed.
   Delete successfully. # Data has been removed.
   ```
#### 开启身份验证
默认情况下，Milvus 的身份验证是关闭的。如果你的配置出现连接问题，可能需要在 Milvus 服务端开启身份验证：
1. 找到 Milvus 的配置文件 user.yaml（在你之前安装的 D:\Milvus\ 目录下）
2. 添加以下配置：
   ```
   # Extra config to override default milvus.yaml
   common:
     security:
       authorizationEnabled: true
   ```
3. 重启 Milvus：./standalone.bat restart

### 工具 Attu
官网: https://github.com/zilliztech/attu
#### Quick Start
Docker (recommended)
```
docker run -d --name attu `
-p 3000:3000 `
-e MILVUS_ADDRESS=host.docker.internal:19530 `
-v attu-data:/data `
zilliz/attu:v3.0.0-beta.6
```
Open http://localhost:3000 and connect to Zilliz Cloud or your open-source Milvus 3.x instance.

The Docker image stores its SQLite database at /data/attu.db by default. The -v attu-data:/data volume persists your saved connections, agent conversations, and preferences across container restarts.

## Neo4j
官网: https://neo4j.ac.cn/docs/

### 使用 Neo4j Docker 镜像
您可以使用以下命令启动 Neo4j 容器。请注意，此 Neo4j 容器在重启后不会保留数据，并且将使用默认的用户名/密码。
```
docker run `
    --publish=7474:7474 --publish=7687:7687 `
    neo4j:2026.03.1
```
您可以通过在浏览器中打开 https://:7474/（Neo4j 的浏览器界面）来测试您的 Neo4j 容器。默认情况下，Neo4j 需要身份验证，并在首次连接时提示您使用 neo4j/neo4j 的用户名/密码登录。随后，系统会提示您设置一个新密码。

### 重启后保留数据
--volume 选项将本地文件夹映射到容器，您可以在其中保留重启后的数据。要在容器之间持久化数据库内容，请在启动容器时将卷挂载到 /data 目录。
```
docker run `
    --publish=7474:7474 --publish=7687:7687 `
    --env NEO4J_AUTH=neo4j/neo4jneo4j `
    --volume=D:\Neo4j\data:/data `
    neo4j:2026.03.1
```

## Redis

### 创建目录
D:\Redis\data

### 启动 Redis
```
docker run -d `
  --name redis-stack `
  -p 6380:6379 `
  -p 8001:8001 `
  -v "D:\Redis\data:/data" `
  redis/redis-stack:latest
```

## Rerank

### 架构（工业标准）
```
Query
  ↓
Recall（Milvus + Neo4j）
  ↓
Fusion（vector + graph）
  ↓
BGE Rerank（核心）
  ↓
TopK → LLM
```

### 部署方案

推荐模型（中文 + 英文通用）：`BAAI/bge-reranker-v2-m3`

#### 方案一：Infinity（推荐，国内友好）

GitHub 仓库：https://github.com/michaelfeil/infinity

> Docker Hub 镜像，国内可直接拉取，API 兼容 TEI `/rerank` 端点

```powershell
# 拉取镜像
docker pull michaelf34/infinity:latest

# 启动（CPU，端口 8002，需设置 HF_ENDPOINT 以支持国内模型下载）
docker run -d --name infinity-rerank -p 8002:7997 `
  -e HF_ENDPOINT=https://hf-mirror.com `
  michaelf34/infinity:latest v2 `
  --model-id BAAI/bge-reranker-v2-m3
```

#### 方案二：HuggingFace TEI

> 官方 Docker 镜像，Rust 实现，性能比 Python 快 3-5 倍
> 镜像托管在 ghcr.io，国内可能需要代理拉取

```powershell
# 如果有代理，在 PowerShell 临时设置：
$env:HTTP_PROXY="http://127.0.0.1:7890"
$env:HTTPS_PROXY="http://127.0.0.1:7890"
docker pull ghcr.io/huggingface/text-embeddings-inference:latest

# GPU 版本
docker run -d --name tei-rerank --gpus all -p 8002:80 `
  -v D:/Rerank/data:/data `
  ghcr.io/huggingface/text-embeddings-inference:latest `
  --model-id BAAI/bge-reranker-v2-m3

# CPU 版本
docker run -d --name tei-rerank -p 8002:80 `
  -v D:/Rerank/data:/data `
  ghcr.io/huggingface/text-embeddings-inference:cpu-latest `
  --model-id BAAI/bge-reranker-v2-m3
```

#### API 接口

##### Infinity API

- 端点：`POST http://localhost:8002/rerank`
- 请求：
```json
{
  "query": "甲醇泄漏如何处理",
  "documents": ["甲醇属于易燃液体...", "泄漏时应立即疏散..."]
}
```
- 响应：
```json
{
  "results": [
    {"relevance_score": 0.95, "index": 0},
    {"relevance_score": 0.82, "index": 1}
  ]
}
```

##### TEI API

- 端点：`POST http://localhost:8002/rerank`
- 请求：
```json
{
  "query": "甲醇泄漏如何处理",
  "texts": ["甲醇属于易燃液体...", "泄漏时应立即疏散..."]
}
```
- 响应：
```json
[
  {"index": 0, "score": 0.95},
  {"index": 1, "score": 0.82}
]
```

#### Spring 配置（application.yml，可选）
```yaml
rag:
  rerank:
    url: http://localhost:8002/rerank
```

## MinerU

MinerU提供了便捷的docker部署方式，这有助于快速搭建环境并解决一些棘手的环境兼容问题。

### 使用 Dockerfile 构建镜像

``` 
wget https://gcore.jsdelivr.net/gh/opendatalab/MinerU@master/docker/china/Dockerfile
docker build -t mineru:latest -f Dockerfile . 
```

### 启动 Docker 容器

```
docker run --gpus all \
  --shm-size 32g \
  -p 30000:30000 -p 7860:7860 -p 8000:8000 -p 8002:8002 \
  --ipc=host \
  -it mineru:latest \
  /bin/bash
```

### 本地 OCR + 远端 VLM 方案（推荐，显存 <8GB）

当本地 GPU 显存不足以运行 VLM 推理时，可使用 `vlm-http-client` 后端将 VLM 推理委托给云端（如通义千问 Qwen-VL），本地仅负责 OCR 和版面分析。

#### 启动 mineru-api
> mineru[core]仅后端 API
> 1. 进入容器手动启动服务
> 
> `docker exec -it mineru bash`
> 
> 2. 进入容器后执行
> #API服务对外开放局域网
> 
> `mineru-api --host 0.0.0.0 --port 30000 &`

```bash
mineru-api --host 0.0.0.0 --port 8000 \
  -b vlm-http-client \
  -u https://dashscope.aliyuncs.com/compatible-mode/v1 \
  --server-headers '{"Authorization":"Bearer <YOUR_API_KEY>"}' \
  --vlm-model-name qwen-vl-plus
```

#### VLM 后端选择参考

| 后端 | 显存要求 | 特点 |
|------|---------|------|
| vlm-vllm-engine | 8GB+ | vllm 加速，速度快 |
| vlm-sglang-engine | 8GB+ | sglang 加速 |
| vlm-lmdeploy-engine | 8GB+ | lmdeploy 加速 |
| vlm-transformers | 2-4GB | HuggingFace 直接推理，速度慢 |
| vlm-http-client | ~2GB | 委托云端，推荐低显存场景 |
| pipeline（纯 OCR） | CPU 可运行 | 无 VLM，精度 86.47 vs VLM 95.39 |

#### Spring AI 调用方式

调用链路：`Spring AI → MinerU API(8000) → vlm-http-client → Qwen-VL 云端`

```
// 同步解析
POST /file_parse   // multipart: files + return_md=true

// 异步任务
POST /tasks        // multipart → 返回 task_id
GET  /tasks/{taskId}/result  // 轮询结果

// 健康检查
GET  /health
```

> **注意**：端口 30000 是 MinerU 内部 VLM 推理用的 OpenAI Server，Spring AI 不应直接调用该端口做文档解析。

调用链路总结

```
前端 / Postman
↓  POST /api/doc/parse（上传 PDF）
Spring AI（WebClient）
↓  POST /file_parse（multipart）
MinerU Docker（mineru-api:8000）
↓  vlm-http-client（OpenAI 兼容协议）
Qwen-VL（dashscope.aliyuncs.com）
↓  返回 VLM 推理结果
MinerU 后处理（版面分析 + 结构化）
↓  返回 Markdown + content_list
Spring AI → 前端
```

> **注意**：mineru-api 启动时通过 -b vlm-http-client + -u 参数将 VLM 推理委托给云端，本地 Docker 不需要 GPU 做 VLM 推理，仅负责 OCR 预处理和后处理编排。


## Chinese-CLIP FastAPI Docker 部署指南

基于 [Chinese-CLIP](https://github.com/OFA-Sys/Chinese-CLIP) 模型封装的 FastAPI 推理服务，支持 GPU（CUDA）和 CPU 两种运行模式。

### 快速开始（使用预构建镜像）

1. 导入镜像

```bash
docker load -i chinese-clip-latest.tar
```

验证导入成功：

```bash
docker images chinese-clip:latest
```

2. 启动服务

**GPU 模式**（推荐，需要 NVIDIA GPU + [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html)）：

```bash
docker run -d \
  --name chinese-clip-service \
  --gpus all \
  -p 8000:8000 \
  -v ./models:/app/models \
  -e MODEL_NAME=ViT-B-16 \
  -e USE_MODELSCOPE=true \
  chinese-clip:latest
```

**CPU 模式**（无 GPU 环境）：

```bash
docker run -d \
  --name chinese-clip-service \
  -p 8000:8000 \
  -v ./models:/app/models \
  -e MODEL_NAME=ViT-B-16 \
  -e USE_MODELSCOPE=true \
  chinese-clip:latest
```

3. 验证服务

```bash
# 健康检查
curl http://localhost:8000/health

# 查看交互式 API 文档
# 浏览器打开 http://localhost:8000/docs
```

> 首次调用模型相关接口时会自动下载模型文件（ViT-B-16 约 900MB），请耐心等待。

---

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MODEL_NAME` | `ViT-B-16` | 模型规模，见下方可选值 |
| `DOWNLOAD_ROOT` | `/app/models` | 模型存储路径 |
| `USE_MODELSCOPE` | `true` | `true` 从 ModelScope 下载，`false` 从 HuggingFace 下载 |
| `PORT` | `8000` | 服务端口 |

#### 可选模型

| 模型 | 参数量 | 特征维度 | 说明 |
|------|--------|---------|------|
| `ViT-B-16` | 188M | 512 | 默认，速度/精度平衡 |
| `ViT-L-14` | 406M | 768 | 大模型，精度更高 |
| `ViT-L-14-336` | 406M | 768 | 336px 输入，细粒度更好 |
| `ViT-H-14` | 958M | 1024 | 最大模型 |
| `RN50` | 77M | 1024 | ResNet 骨干，速度快 |

---

### API 接口

`GET /health` — 健康检查

```bash
curl http://localhost:8000/health
```

返回：
```json
{
  "status": "ok",
  "device": "cuda",
  "model": "ViT-B-16",
  "available_models": ["ViT-B-16", "ViT-L-14", "ViT-L-14-336", "ViT-H-14", "RN50"]
}
```

`POST /encode/text` — 文本特征提取

```bash
curl -X POST http://localhost:8000/encode/text \
  -H "Content-Type: application/json" \
  -d '{"texts": ["一只可爱的猫", "美丽的风景"]}'
```

`POST /encode/image` — 图像特征提取（文件上传）

```bash
curl -X POST http://localhost:8000/encode/image \
  -F "file=@photo.jpg"
```

`POST /encode/image_base64` — 图像特征提取（Base64）

```bash
curl -X POST http://localhost:8000/encode/image_base64 \
  -F "image_base64=<base64字符串>"
```

`POST /similarity` — 图文相似度计算（文件上传）

```bash
curl -X POST http://localhost:8000/similarity \
  -F "file=@photo.jpg" \
  -F "texts=一只猫" \
  -F "texts=一条狗" \
  -F "texts=风景照"
```

`POST /similarity_base64` — 图文相似度计算（Base64）

```bash
curl -X POST http://localhost:8000/similarity_base64 \
  -F "image_base64=<base64字符串>" \
  -F "texts=一只猫" \
  -F "texts=一条狗"
```

`POST /zeroshot` — 零样本图像分类（文件上传）

```bash
curl -X POST http://localhost:8000/zeroshot \
  -F "file=@photo.jpg" \
  -F "labels=猫" \
  -F "labels=狗" \
  -F "labels=鸟"
```

`POST /zeroshot_base64` — 零样本图像分类（Base64）

```bash
curl -X POST http://localhost:8000/zeroshot_base64 \
  -F "image_base64=<base64字符串>" \
  -F "labels=猫" \
  -F "labels=狗"
```


### 重新构建镜像（可选）

如果需要从源码重新构建：

```bash
# GPU 加速构建
set DOCKER_BUILDKIT=0
docker build --pull=false -t chinese-clip:latest .

# 导出镜像
docker save chinese-clip:latest -o chinese-clip-latest.tar
```

---

### 常见问题

**Q: 首次请求响应很慢？**
A: 首次调用任何模型接口时会自动下载模型权重（ViT-B-16 约 900MB），后续请求会使用缓存。

**Q: GPU 模式报错 "could not select device driver nvidia"？**
A: 需要安装 [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html)，或改用 CPU 模式启动。

**Q: 如何更换模型？**
A: 启动时设置环境变量 `MODEL_NAME`，例如 `docker run -e MODEL_NAME=ViT-L-14 ...`。

**Q: 模型下载失败？**
A: 默认从 ModelScope 下载（国内友好）。如需切换，设置 `USE_MODELSCOPE=false` 改用 HuggingFace。


## LlamaFactory Docker 部署指南

- 官网文档: https://llamafactory.readthedocs.io/zh-cn/latest/
- GitHub: https://github.com/hiyouga/LLaMA-Factory

### Docker 部署与启动（GPU 加速，持久化数据不丢失）

#### 1. 新建数据卷

> 训练数据、输出结果和模型缓存存在 Docker 虚拟磁盘，重装容器不丢数据

```powershell
docker volume create llamafactory-hf-cache
docker volume create llamafactory-shared-data
docker volume create llamafactory-output
```

#### 2. 启动 LlamaFactory 容器

```powershell
docker run -d `
  --gpus all `
  --name llamafactory `
  --restart always `
  --shm-size 16g `
  --ipc host `
  -p 7860:7860 -p 8000:8000 `
  -v llamafactory-hf-cache:/root/.cache/huggingface `
  -v llamafactory-shared-data:/app/shared_data `
  -v llamafactory-output:/app/output `
  hiyouga/llamafactory:latest `
  llamafactory-cli webui
```

参数说明：

| 参数 | 说明 |
|------|------|
| `-d` | 后台常驻运行 |
| `--gpus all` | 直通 GPU 显卡 |
| `--name llamafactory` | 容器名称，便于管理和引用 |
| `--restart always` | 容器退出后自动重启 |
| `--shm-size 16g` | 共享内存 16G，防止 DataLoader 共享内存不足 |
| `--ipc host` | 使用宿主机 IPC 命名空间，提升多进程通信性能 |
| `-p 7860:7860` | WebUI 访问端口 |
| `-p 8000:8000` | API 服务端口 |
| `-v llamafactory-hf-cache:/root/.cache/huggingface` | 使用宿主机的 Hugging Face 缓存文件夹 |
| `-v llamafactory-shared-data:/app/shared_data` | 宿主机中存放数据集的文件夹路径 |
| `-v llamafactory-output:/app/output` | 将导出目录设置为该路径后，即可在宿主机中访问导出后的模型 |

### 常用操作命令

#### 验证服务

```powershell
# WebUI：浏览器打开 http://localhost:7860
```


## Ollama

### Docker 部署与启动（GPU 加速，持久化模型不丢失）

#### 1. 新建数据卷

> 模型存在 Docker 虚拟磁盘 E 盘，重装容器不丢权重

```powershell
docker volume create ollama-data
```

#### 2. 启动 Ollama 容器

```powershell
docker run -d `
  --gpus all `
  --name ollama `
  --restart always `
  -p 11434:11434 `
  -v ollama-data:/root/.ollama `
  ollama/ollama
```

参数说明：

| 参数 | 说明 |
|------|------|
| `-d` | 后台常驻运行 |
| `--gpus all` | 直通 RTX3050 显卡 |
| `-p 11434:11434` | 宿主机访问端口，浏览器 / 其他程序可调用接口 |
| `-v ollama-data:/root/.ollama` | 模型持久化存储，所有下载的大模型存在 E 盘 Docker 虚拟磁盘 |

### 常用操作命令

#### 拉取模型

> 进入容器终端拉模型（qwen2.5:7b 适配 8G 显存）

```powershell
docker exec -it ollama ollama pull qwen2.5:7b
```

#### 本地 Windows 直接调用 API（不用进容器）

```powershell
curl http://localhost:11434/api/chat -d '{
  "model":"qwen2.5:7b",
  "messages":[{"role":"user","content":"你好"}]
}'
```

#### 停止 / 重启 / 删除容器

```powershell
# 停止
docker stop ollama

# 重启
docker restart ollama

# 删除容器（模型还在 ollama-data 卷里不会丢）
docker rm ollama
```

## MinIO

官网: https://www.minio.org.cn/

### Docker 部署与启动（持久化数据不丢失）

#### 1. 新建数据卷

> 对象存储数据存在 Docker 虚拟磁盘，重装容器不丢数据

```powershell
docker volume create minio-data
```

#### 2. 启动 MinIO 容器

```powershell
docker run -d `
  --name minio `
  --restart always `
  -p 9000:9000 -p 9001:9001 `
  -v minio-data:/data `
  -e MINIO_ROOT_USER=admin `
  -e MINIO_ROOT_PASSWORD=minio123456 `
  minio/minio server /data --console-address ":9001"
```

参数说明：

| 参数 | 说明 |
|------|------|
| `-d` | 后台常驻运行 |
| `--name minio` | 容器名称，便于管理和引用 |
| `--restart always` | 容器退出后自动重启 |
| `-p 9000:9000` | S3 API 端口，应用程序通过此端口访问对象存储 |
| `-p 9001:9001` | Web Console 端口，浏览器访问管理界面 |
| `-v minio-data:/data` | 数据持久化存储，所有对象存在 Docker 虚拟磁盘 |
| `-e MINIO_ROOT_USER=admin` | 管理员用户名 |
| `-e MINIO_ROOT_PASSWORD=minio123456` | 管理员密码（生产环境请使用强密码） |

### 常用操作命令

#### 验证服务

```powershell
# Web Console：浏览器打开 http://localhost:9001
# API 端点：http://localhost:9000
```

