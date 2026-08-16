<div align="center">

# TravelMind

**智能旅行规划系统 · Intelligent Travel Planning System**

一个集 AI 行程规划、视觉风险检测、向量记忆检索于一体的全栈旅行规划平台

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-42b883.svg)](https://vuejs.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.x-009688.svg)](https://fastapi.tiangolo.com/)
[![Python](https://img.shields.io/badge/Python-3.10+-blue.svg)](https://www.python.org/)
[![License](https://img.shields.io/badge/License-GPL--2.0-red.svg)](./LICENSE)
[![Version](https://img.shields.io/badge/Version-1.1.0-purple.svg)](./docs/deploy/release-notes-v1.1.0.md)

</div>

---

## Overview

TravelMind 是一个分离式全栈智能旅行规划系统，包含面向消费者的旅行应用和独立的管理后台。Java 后端负责编排与持久化，Python AI 服务提供本地化的视觉检测与舒适度评分，前端基于 Vue 3 构建双端界面。

```
Vue Consumer ──> /api/public/** + /api/user/** ──┐
Vue Admin    ──> /api/admin/**                  ─┼──> Domain Services ──> MySQL
                                                   ├──> Redis / Cache
                                                   ├──> AI / Map / Content Providers
                                                   └──> FastAPI Python AI (REST)
```

---

## Features

### Core Modules

| Module | Description |
|--------|-------------|
| AI Trip Planning | 多智能体协作规划，支持 DeepSeek 大模型接入与本地 Demo Planner 降级 |
| Travel Risk Detection | 自训练 TravelRisk-YOLO 模型，实时识别旅行图片中的风险要素 |
| Travel Comfort Scoring | 自训练 TravelComfort 回归模型，量化评估行程舒适度 |
| Vector Memory | 基于 BAAI/bge-small-zh-v1.5 + Qdrant 的旅行记忆语义检索 |
| Intelligent Recommendation | 个性化推荐引擎，支持语义搜索与相似资源发现 |
| Travel Journal | 游记发布与管理，支持图文混排与地理位置标注 |
| Interactive Map | 基于 MapLibre GL 的 3D 旅行地图，支持城市切换与轨迹编辑 |
| Community | 旅行社区互动，支持帖子、评论、点赞与通知 |
| Admin Console | 独立管理后台，包含数据看板与系统配置 |

### Highlights

- **Dual-Portal Architecture** - 消费端与管理端路由隔离，JWT 角色双向隔离
- **Self-Trained Models** - 内置 YOLO 与 Comfort 模型，训练报告与证据完整
- **Graceful Degradation** - Python AI 故障不阻塞行程规划；缺失外部配置自动降级到本地 Demo 模式
- **Zero-Config Demo** - 开箱即用的免费演示模式，无需任何 API Key 即可运行

---

## Tech Stack

### Backend

| Technology | Purpose |
|------------|---------|
| Java 17 + Spring Boot 3 | 应用框架与 REST 控制器 |
| Maven Multi-Module | 模块化构建（app / common / modules） |
| MyBatis-Plus | ORM 与数据库访问 |
| Redis 7 | 会话管理、缓存与限流 |
| Sa-Token + JWT | 身份认证与角色鉴权 |
| Flyway | 数据库版本迁移（V2 ~ V19） |

### Frontend

| Technology | Purpose |
|------------|---------|
| Vue 3 + Vite 7 | 响应式 UI 框架与构建工具 |
| Vue Router 4 | 双端路由（消费者 + 管理员） |
| MapLibre GL | 3D 旅行地图渲染 |
| Axios | HTTP 客户端 |
| Lucide Icons | 图标库 |

### Python AI

| Technology | Purpose |
|------------|---------|
| FastAPI | AI 服务 REST API |
| Ultralytics YOLO | TravelRisk-YOLO 目标检测 |
| Scikit-learn | TravelComfort 舒适度回归 |
| BAAI/bge-small-zh-v1.5 | 中文语义嵌入模型 |
| Qdrant 1.15 | 向量数据库（512 维余弦检索） |

### Infrastructure

| Technology | Purpose |
|------------|---------|
| MySQL 8.0 | 主数据库 |
| Docker Compose | 容器化部署 |
| Nginx | 生产环境前端代理 |

---

## Project Structure

```
travelmind/
├── app/                        # Spring Boot 启动模块与 REST 控制器
│   └── src/main/resources/db/  # Flyway 迁移脚本 (V2 ~ V19)
├── common/                     # 共享模块 (JSON / Web / MyBatis / Redis / Sa-Token)
├── modules/
│   ├── identity/               # 身份认证与用户/管理员角色
│   ├── resources/              # 资源 CRUD、社区、游记、记忆库
│   ├── trip/                   # 行程规划与 AI 编排
│   ├── ai/                     # 大模型 Agent 与 Prompt 管理
│   ├── map/                    # 地图服务（高德 API）
│   └── content/                # 旅行内容（小红书抓取）
├── frontend/                   # Vue 3 前端应用
│   └── src/
│       ├── views/              # 页面组件（消费者 + 管理员）
│       ├── components/         # 通用组件（地图、上传、动画）
│       ├── api/                # API 客户端
│       └── composables/        # 组合式函数
├── python-ai/                  # FastAPI AI 服务
│   ├── app/
│   │   └── main.py             # 健康检查、视觉检测、舒适度评分
│   └── models/                 # 内置模型文件
│       ├── travel-risk-yolo-best.pt
│       └── travel-comfort-v1.joblib
├── docs/                       # 交付文档
│   ├── design/                 # 系统设计文档
│   ├── api/                    # API 文档
│   ├── ai/                     # AI 训练报告与证据
│   ├── test/                   # 测试计划与报告
│   └── deploy/                 # 部署指南与发布说明
├── compose.yml                 # 开发环境 Docker Compose
├── compose.prod.yml            # 生产环境 Docker Compose
└── start.ps1                   # 一键启动脚本
```

---

## Quick Start

### Prerequisites

| Requirement | Version |
|-------------|---------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| Python | 3.10+ |
| MySQL | 8.0+ |
| Redis | 7+ |

### One-Click Start (Windows)

```powershell
.\start.ps1
```

脚本会自动完成：创建 `.env` -> 启动 MySQL/Redis/Qdrant -> 构建 Java 后端 -> 启动 Python AI -> 启动 Vue 前端 -> 健康检查 -> 打开浏览器

```powershell
.\start.ps1 -NoBrowser    # 不自动打开浏览器
.\start.ps1 -Stop         # 停止所有服务
```

### Access Points

| Endpoint | URL |
|----------|-----|
| Consumer App | http://localhost:5173/ |
| Admin Console | http://localhost:5173/admin |
| Backend API | http://localhost:8080/health |
| Python AI | http://localhost:19080/health |

### Demo Accounts

| Role | Username | Password |
|------|----------|----------|
| Consumer | `demo_user` | `travel123` |
| Admin | `admin` | `admin123` |

> Demo 账户在首次启动时由 BCrypt 自动创建。可通过 `TRAVELMIND_DEMO_PASSWORD` 和 `TRAVELMIND_ADMIN_PASSWORD` 环境变量覆盖默认密码。

---

## Manual Setup

### Backend

```bash
mvn -pl app -am package -DskipTests
mvn -pl app -am spring-boot:run
```

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

### Python AI

```bash
cd python-ai
python -m venv .venv
.venv/Scripts/python.exe -m pip install -r requirements.txt
.venv/Scripts/python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 19080
```

---

## Configuration

复制 `.env.example` 为 `.env` 并填写配置：

```bash
cp .env.example .env
```

### Key Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | JWT 签名密钥（生产环境必须设置 32+ 字节） | dev-only-fallback |
| `JWT_TTL_SECONDS` | Token 有效期 | 2592000 (30 days) |
| `MEMORY_SERVICE_TOKEN` | Java-Python 内部服务令牌 | dev-only-fallback |
| `QDRANT_URL` | Qdrant 向量数据库地址 | http://localhost:6333 |
| `AI_OPENAI_API_KEY` | 大模型 API Key（留空则使用 Demo 模式） | - |
| `AMAP_ENABLED` | 高德地图服务开关 | false |
| `XHS_ENABLED` | 小红书内容抓取开关 | false |

> **Free Demo Mode**: 不配置任何 API Key 即可运行。系统自动降级为本地规划 + 自训练模型，完整体验核心功能。

---

## AI Models

| Model | Type | File | Purpose |
|-------|------|------|---------|
| TravelRisk-YOLO | Object Detection | `python-ai/models/travel-risk-yolo-best.pt` | 旅行图片风险要素识别 |
| TravelComfort | Regression | `python-ai/models/travel-comfort-v1.joblib` | 行程舒适度量化评分 |
| bge-small-zh-v1.5 | Text Embedding | HuggingFace (auto-download ~100MB) | 中文语义向量化 |

训练报告与混淆矩阵等证据见 `docs/ai/` 目录。

---

## Database

使用 Flyway 管理 19 个迁移版本（V2 ~ V19），涵盖：

| Migration | Content |
|-----------|---------|
| V2 | 基础 CRUD 表结构 |
| V4 | 身份与角色表 |
| V5 | 社区与助手表 |
| V9 | 旅行记忆表 |
| V10 | 记忆知识库 |
| V11 | 地图 POI 目录 |
| V12 | 舒适度反馈表 |
| V18 | 用户通知表 |
| V19 | 游记与推荐引擎表 |

---

## Testing

```bash
# Java - 24 tests
mvn test

# Python - 6 tests (含真实 YOLO 推理)
cd python-ai && .venv/Scripts/python.exe -m pytest

# Frontend - 8 tests
cd frontend && npm test
```

---

## Production Deployment

```bash
# 1. 填写 .env 中所有必需密钥
# 2. 构建并启动
docker compose -f compose.prod.yml up -d --build
```

生产环境仅暴露 Nginx 前端代理；MySQL、Redis、Qdrant、Java 和 Python 服务均在内部网络。Flyway 自动迁移数据库并在无管理员时创建初始管理员账户。

---

## Documentation

| Document | Path |
|----------|------|
| System Architecture | `docs/design/system-architecture.md` |
| Requirements Spec | `docs/design/requirements-specification.md` |
| Database Design | `docs/design/database-design.md` |
| Module Design | `docs/design/module-design.md` |
| Backend API | `docs/api/backend-api.md` |
| Java-Python API | `docs/api/java-python-api.md` |
| AI Training Report | `docs/ai/training-report.md` |
| Test Report | `docs/test/test-report.md` |
| Deployment Guide | `docs/deploy/deployment-guide.md` |
| Release Notes v1.1.0 | `docs/deploy/release-notes-v1.1.0.md` |

---

## License

本项目采用 [GPL-2.0](./LICENSE) 许可证。

---

<div align="center">

**Built with Spring Boot, Vue 3 & FastAPI**

</div>
