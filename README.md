# MokaDoc 智能文档系统

基于 **RAG（检索增强生成）** 的智能文档问答平台：上传文档 → 向量化索引 → 选定文档流式问答 → 引用溯源 → 构建知识图谱。前后端分离，单仓库（monorepo）结构。

[![Java](https://img.shields.io/badge/Java-17-blue)](backend/pom.xml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.12-6DB33F)](backend/pom.xml)
[![Vue](https://img.shields.io/badge/Vue-3.5-42b883)](frontend/package.json)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF)](frontend/package.json)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境准备](#环境准备)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [核心概念](#核心概念)
- [API 文档](#api-文档)
- [测试](#测试)
- [设计文档](#设计文档)
- [已知限制](#已知限制)
- [许可证](#许可证)

---

## 功能特性

### 文档管理
- 多格式解析：PDF、DOCX、XLSX/XLS、TXT、MD、JSON、CSV、JAVA、VUE、JS、TS、XML、YML 等
- **MD5 秒传查重**：同一用户重复文件直接识别，前端可预检 `GET /document/check-md5`
- 上传路径 `uploads/documents/{userId}/{docId}/`，上传完成即触发异步向量化
- 逻辑删除（`delete_flag`）、预览、下载、重建索引

### 智能问答（RAG）
- **SSE 流式输出**（打字机效果），事件协议：`connected → streaming → complete/error`
- 会话管理：创建 / 重命名 / 删除 / 分页加载，会话可绑定多篇文档作为问答上下文
- **引用溯源**：每条 AI 回答可回查原文片段（`GET /chat/message/{messageId}/references`）
- 聊天记忆：Redis（L1 缓存）+ MySQL（L2 持久化）双层，滑动窗口保留最近 20 条

### 进阶 RAG 管道
- **多策略查询转换**（`compress` / `expand` / `multi_strategy`）
- **智能路由**：LLM 意图分析（SEMANTIC / KEYWORD / RELATION / GRAPH / HYBRID）+ target 查表
- **混合检索**：向量检索（ES kNN）+ BM25 全文检索，**RRF（Reciprocal Rank Fusion）** 融合重排
- **文本增强**：滑动窗口摘要（sliding-window）、图谱实体注入（graph-entity）
- **分块策略**：coarse / fine / dual / article-aware（按文章结构感知分块）
- **检索扩展**：邻居块扩展（±window）、父子块解析（fine 命中 → 引入 coarse 父块）
- 内容签名缓存：文件 SHA256 未变则跳过重复索引

### 知识图谱
- LLM 批量抽取实体与关系 → 实体对齐（同名 / 近名合并）→ MySQL 图谱表存储
- SSE 实时推送构建进度（批次级百分比），AI 抽取失败时本地术语抽取兜底
- 图谱检索接入 RAG 管道：子图扩展 → 文本化注入 Prompt
- 前端可视化：vis-network 图谱渲染 + echarts 统计图表 + 知识脑图（树形）

### 用户与权限
- 注册 / 登录 / 注销 / 资料修改 / 密码修改，基于 **Redis Session**（Cookie 会话）
- 操作日志、存储空间摘要
- `@AuthCheck` AOP 注解（角色校验暂未启用，等待 `user_role` 字段）

---

## 技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.5.12、Java 17 |
| ORM | MyBatis-Flex 1.11.1 + HikariCP |
| 数据库 | MySQL 8.x |
| 缓存 / Session | Redis（Spring Session Data Redis、LangChain4j 记忆缓存） |
| AI 框架 | LangChain4j 1.13.x（OpenAI 兼容接口，默认 DeepSeek） |
| 向量 / 全文检索 | Elasticsearch（向量 kNN + BM25 混合检索） |
| 本地 Embedding | all-MiniLM-L6-v2 量化版（384 维，ONNX，无需外部 Embedding 服务） |
| 中文分词 | HanLP（查询扩展实体识别） |
| 文档解析 | Apache PDFBox 3、Apache POI 5 |
| 流式接口 | Spring MVC SseEmitter（后端）/ fetch + ReadableStream（前端） |
| API 文档 | Knife4j / SpringDoc OpenAPI 3 |
| 前端框架 | Vue 3.5 + TypeScript + Vite 8 |
| 状态 / 路由 | Pinia 3、Vue Router 5 |
| UI / 图表 | Ant Design Vue 4（message / notification）、ECharts 5、vis-network 7 |
| Markdown | marked |

---

## 项目结构

```text
MokaDoc/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd               # Maven Wrapper（无需本地安装 Maven）
│   ├── sql/table.sql                 # 完整建库建表脚本（12 张表）
│   ├── docs/                         # 架构与方案设计文档
│   ├── uploads/                      # 上传文件运行时目录（不入库）
│   └── src/
│       ├── main/java/com/kanade/backend/
│       │   ├── ai/                   # AI 服务接口、工厂
│       │   │   ├── memory/           # Redis + DB 双层聊天记忆
│       │   │   └── rag/              # RAG 编排、检索、分块、增强、注入
│       │   │       ├── orchestrator/ # AdvancedRagOrchestrator（核心管道）
│       │   │       ├── router/       # SmartQueryRouter 智能路由
│       │   │       ├── transformer/  # 查询转换（压缩 / 扩展 / 多策略）
│       │   │       ├── aggregator/   # RRF 融合
│       │   │       ├── embedding/    # 分块策略 + 文本增强器
│       │   │       └── injector/     # 内容注入 + 图谱注入
│       │   ├── graph/                # 知识图谱构建、对齐、检索
│       │   ├── controller/           # User / Document / Chat / Graph
│       │   ├── service/              # 业务接口与实现
│       │   ├── entity/ mapper/ dto/  # 数据层
│       │   ├── config/ aop/ sse/     # 配置、鉴权切面、SSE 连接池
│       │   └── exception/ common/    # 统一响应与异常
│       └── main/resources/
│           ├── application.yml       # 主配置（含 RAG 各开关）
│           ├── application-local.yml # 本地敏感配置（不入库）
│           └── schema.sql            # 图谱表自动初始化 DDL
│
└── frontend/                         # Vue 3 前端
    ├── package.json
    ├── vite.config.ts                # /api 代理 → localhost:8080
    ├── openapi2ts.config.ts          # 由后端 OpenAPI 生成 API 客户端
    └── src/
        ├── api/                      # 自动生成的接口（4 个模块）
        ├── views/                    # 页面
        │   ├── LoginView.vue         # 登录 / 注册
        │   ├── DocumentManagement.vue# 文档列表 / 上传 / 重建索引
        │   ├── IntelligentQA.vue     # 流式问答 + 引用溯源
        │   ├── KnowledgeGraph.vue    # 图谱构建 + 可视化
        │   ├── DocumentPreview.vue   # 文档预览 / 下载
        │   └── UserCenter.vue        # 个人中心
        ├── components/MainLayout.vue # 整体布局（侧边栏 + 顶栏）
        ├── stores/user.ts            # 登录态
        ├── router/index.ts           # 路由 + 登录守卫
        ├── utils/sse.ts              # SSE 封装（EventSource / fetch）
        └── request.ts                # Axios 实例 + 401 拦截
```

---

## 环境准备

| 依赖 | 版本要求 | 必需 | 用途 |
|---|---|---|---|
| JDK | 17+（编译目标为 17） | ✅ | 后端运行 |
| Maven | 3.9+（或直接用 `mvnw`） | ✅ | 后端构建 |
| Node.js | `^20.19.0 \|\| >=22.12.0` | ✅ | 前端构建 |
| MySQL | 8.x | ✅ | 业务数据 |
| Redis | 6+ | ✅ | Session + 聊天记忆 |
| Elasticsearch | 8.x | ✅ | 向量与全文检索 |
| DeepSeek API Key | — | ✅ | LLM 对话与图谱抽取 |

> 前端 `npm install` 后即可，无需全局安装 Vue CLI。

---

## 快速开始

### 1. 初始化数据库

```bash
mysql -uroot -p < backend/sql/table.sql
```

脚本会创建数据库 `mokaDoc` 并建好 12 张表（用户、文档、问答会话、消息、引用、知识图谱等）。
图谱相关的 3 张表（`graph_task` / `graph_node` / `graph_edge`）也会由 Spring Boot 启动时执行 `schema.sql` 自动确保存在。

### 2. 启动基础设施

确保以下服务在本机运行：

| 服务 | 端口 |
|---|---|
| MySQL | 3306 |
| Redis | 6379 |
| Elasticsearch | 9200 |

### 3. 配置敏感信息

新建 `backend/src/main/resources/application-local.yml`（已被 `.gitignore` 忽略，**不会提交**），填入自己的密钥：

```yaml
langchain4j:
  open-ai:
    streaming-chat-model:
      base-url: https://api.deepseek.com
      api-key: sk-xxxxxxxx        # ← 换成你的 DeepSeek API Key
      model-name: deepseek-v4-flash
graph:
  extraction:
    llm:
      base-url: https://api.deepseek.com/
      api-key: sk-xxxxxxxx        # ← 图谱抽取用（可与上面相同）
      model-name: deepseek-v4-flash
```

数据库连接默认读 `application.yml`（`jdbc:mysql://localhost:3306/mokaDoc`，用户 `root`），如不同请在 `application-local.yml` 中覆盖 `spring.datasource`。

### 4. 启动后端

```bash
cd backend
./mvnw spring-boot-starter:run       # Windows: mvnw.cmd spring-boot-starter:run
```

启动成功后：

- 服务地址：<http://localhost:8080/api>
- Knife4j 接口文档：<http://localhost:8080/api/doc.html>
- Swagger UI：<http://localhost:8080/api/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/api/v3/api-docs>

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 <http://localhost:5173>，注册账号后即可使用。

---

## 配置说明

主配置位于 `backend/src/main/resources/application.yml`：

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `server.port` / `server.servlet.context-path` | `8080` / `/api` | 后端端口与上下文 |
| `spring.data.redis` | `localhost:6379`，db 4 | Session 与缓存 |
| `elasticsearch.host/port` | `localhost:9200` | ES 地址 |
| `elasticsearch.index-name` | `rag_documents` | 向量 + 全文索引名 |
| `file.upload.dir` | `./uploads/documents` | 文件存储根目录 |
| `rag.query-transformation.type` | `multi_strategy` | 查询转换：`compress` / `expand` / `multi_strategy` |
| `rag.routing.enabled` | `true` | 智能路由开关 |
| `rag.reranking.model` / `top-k` | `rrf` / `50` | 融合方式与候选数 |
| `rag.es.max-results` / `min-score` | `50` / `0.60` | 检索数量与相似度阈值 |
| `rag.embedding.enhancers` | `sliding-window, graph-entity` | 文本增强器，留空则禁用 |
| `rag.embedding.chunk-strategy` | `article-aware` | `coarse` / `fine` / `dual` / `article-aware` |
| `rag.embedding.neighbor-expansion.window` | `2` | 邻居块扩展窗口 ±N |
| `rag.embedding.parent-child.enabled` | `true` | 父子块解析开关 |

前端环境变量（可选）：

| 变量 | 说明 |
|---|---|
| `VITE_API_BASE_URL` | 覆盖 Axios baseURL，默认 `/api`（配合 Vite 代理） |

---

## 核心概念

### 请求生命周期（以智能问答为例）

```text
用户提问 → POST /api/chat/ask (SSE)
  │
  ├─ 会话校验（Redis Session → MySQL）
  ├─ 若指定 documentIds：
  │    DocumentRagService.getContentRetriever()
  │      → 按用户 + 文档 ID 过滤的 ES 向量检索器
  │
  ├─ AdvancedRagOrchestrator.createAdvancedRagAugmentor()
  │    ① 查询转换（多策略：压缩 / 实体扩展 / 子问题）
  │    ② 智能路由（LLM 意图 → vector / text / graph）
  │    ③ 检索（向量 kNN + BM25 + 图谱子图）
  │    ④ RRF 融合重排（top 50 → 注入前 k）
  │    ⑤ 内容注入（模板 Prompt + 引用收集器）
  │
  ├─ LangChain4j 流式生成 → Flux<String>
  │    每个 chunk 包装为 {"type":"streaming","content":"..."} 经 SSE 下发
  │
  └─ 结束：保存 AI 消息 + 引用 → {"type":"complete",...}
```

### SSE 事件协议

| 事件 | 载荷示例 | 含义 |
|---|---|---|
| `connected` | `{"type":"connected","sessionId":1}` | 连接建立 / 返回会话 ID |
| `streaming` | `{"type":"streaming","content":"你好"}` | 增量文本 |
| `complete` | `{"type":"complete","sessionId":1,"messageId":9}` | 生成结束 |
| `error` | `{"type":"error","message":"..."}` | 出错 |

### 统一响应格式

```json
{ "code": 0, "data": {}, "message": "ok" }
```

| code | 含义 |
|---|---|
| 0 | 成功 |
| 40000 | 参数错误 |
| 40100 | 未登录 |
| 40101 | 无权限 |
| 40400 | 资源不存在 |
| 50000 | 系统异常 |

---

## API 文档

启动后端后访问：

- **Knife4j（推荐）**：<http://localhost:8080/api/doc.html>
- **Swagger UI**：<http://localhost:8080/api/swagger-ui.html>
- **OpenAPI JSON**：<http://localhost:8080/api/v3/api-docs>

四个 Controller 分组：

| 分组 | 前缀 | 主要端点 |
|---|---|---|
| 用户管理 | `/user` | register、login、logout、current、profile、password、logs、storage |
| 文档管理 | `/document` | upload、check-md5、list、`{id}`、`{id}/preview`、`{id}/download`、reindex |
| 聊天管理 | `/chat` | ask（SSE）、session CRUD、chat/list、message/{id}/references |
| 图谱管理 | `/graph` | build、build/progress（SSE）、tasks、search、subgraph、data、stats、mindmap、graph-data |

### 重新生成前端 API 客户端

修改后端接口后，先启动后端，再执行：

```bash
cd frontend
npm run openapi2ts
```

> ⚠️ 重新生成会覆盖手动修改。目前 `wendangguanli.ts` 中的 `upload()` 被手动改为 `multipart/form-data`（自动生成会错设为 JSON），重新生成后需还原该修改（详见 `frontend/CLAUDE.md`）。

---

## 测试

```bash
cd backend

# 全部测试（需 MySQL + Redis 运行；AI 相关用例还需有效 DeepSeek API Key）
./mvnw test

# 单个测试类
./mvnw test -Dtest=GraphExtractionServiceTest

# 仅编译
./mvnw compile -DskipTests
```

测试说明：

- 8 个 `@SpringBootTest` 集成测试需要完整基础设施（MySQL、Redis、ES、LLM Key）
- 3 个纯单元测试（`GraphBatchBuilderTest`、`GraphEntityAlignerTest`、`GraphExtractionServiceTest`）无外部依赖，可直接跑

前端暂无测试框架，类型检查：

```bash
cd frontend
npm run type-check
```

---

## 设计文档

深入的设计与方案文档见 [`backend/docs/`](backend/docs/)：

| 文档 | 主题 |
|---|---|
| [ES+Neo4j双引擎RAG系统-详细实现方案.md](backend/docs/ES%2BNeo4j%E5%8F%8C%E5%BC%95%E6%93%8ERAG%E7%B3%BB%E7%BB%9F-%E8%AF%A6%E7%BB%86%E5%AE%9E%E7%8E%B0%E6%96%B9%E6%A1%88.md) | 双引擎检索架构 |
| [RAG多策略查询转换模块-技术实现方案.md](backend/docs/RAG%E5%A4%9A%E7%AD%96%E7%95%A5%E6%9F%A5%E8%AF%A2%E8%BD%AC%E6%8D%A2%E6%A8%A1%E5%9D%97-%E6%8A%80%E6%9C%AF%E5%AE%9E%E7%8E%B0%E6%96%B9%E6%A1%88.md) | 查询转换 |
| [RAG混合检索实现方案.md](backend/docs/RAG%E6%B7%B7%E5%90%88%E6%A3%80%E7%B4%A2%E5%AE%9E%E7%8E%B0%E6%96%B9%E6%A1%88.md) | 向量 + BM25 混合 |
| [进阶RAG优化功能-完整实现.md](backend/docs/%E8%BF%9B%E9%98%B6RAG%E4%BC%98%E5%8C%96%E5%8A%9F%E8%83%BD-%E5%AE%8C%E6%95%B4%E5%AE%9E%E7%8E%B0.md) | 进阶 RAG 全量实现 |
| [向量化创新点.md](backend/docs/%E5%90%91%E9%87%8F%E5%8C%96%E5%88%9B%E6%96%B0%E7%82%B9.md) | 向量化与分块创新点 |
| [引用溯源功能实现方案.md](backend/docs/%E5%BC%95%E7%94%A8%E6%BA%AF%E6%BA%90%E5%8A%9F%E8%83%BD%E5%AE%9E%E7%8E%B0%E6%96%B9%E6%A1%88.md) | 回答引用链路 |
| [文档向量化管道-技术讲稿.md](backend/docs/%E6%96%87%E6%A1%A3%E5%90%91%E9%87%8F%E5%8C%96%E7%AE%A1%E9%81%93-%E6%8A%80%E6%9C%AF%E8%AE%B2%E7%A8%BF.md) | 向量化管道讲解 |
| [system_architecture.puml](backend/docs/system_architecture.puml) | 系统结构图（PlantUML） |

根目录还包含向量化模块的设计报告（`MokaDoc文档向量化模块设计报告.docx`、`MokaDoc-report-output.docx`）。

---

## 已知限制

- **密码加密**：MD5 + 静态盐，仅适合开发环境，生产请替换为 BCrypt / Argon2
- **角色权限**：`@AuthCheck` 的角色校验逻辑暂未启用（等待 `sys_user.user_role` 字段）
- **API Key**：`application-local.yml` 含真实密钥且被 git 忽略；请勿把该文件提交或分享
- **文件上传上限**：200MB（`spring.servlet.multipart.max-file-size`）
- **扫描版 PDF**：纯图片 PDF 无法提取文字（PDFBox 仅抽取文本层）
- **外部服务**：Elasticsearch / Redis / MySQL 需自行部署，仓库暂不提供 docker-compose

---

## 许可证

[Apache License 2.0](LICENSE)
