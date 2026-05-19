# AgentTest-All-u-Need — 后端

## 项目介绍

AgentTest-All-u-Need 是一个用于评测 AI 对话 Agent 的全栈系统，本仓库为后端服务。

系统通过题库驱动、LLM-as-Judge 裁判的方式，对 **Agent 应用**（如自定义 ChatBot）和 **LLM 基础模型**（如 DeepSeek、GPT）进行自动化评测。支持单轮/多轮对话、多维度加权评分、进度实时轮询和结果对比分析。

**技术栈**: Spring Boot 3.5.14 + MyBatis-Plus 3.5.7 + MySQL 8.0 + Lombok + Hutool + Spring AI

## 功能介绍

### 1. Agent 管理

管理待评测的 Agent 应用，每个 Agent 可独立配置 API 端点、请求模板、响应解析方式和鉴权策略。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/agents` | 分页查询，支持 keyword/type/status 筛选 |
| GET | `/api/agents/{id}` | 查看详情 |
| POST | `/api/agents` | 创建 Agent |
| PUT | `/api/agents/{id}` | 更新 Agent |
| DELETE | `/api/agents/{id}` | 删除 Agent（逻辑删除） |
| POST | `/api/agents/{id}/test-connection` | 测试 Agent API 连通性 |

### 2. LLM 模型管理

管理待评测的基础大语言模型，与 Agent 的区别在于 LLM 使用标准 OpenAI 兼容格式，无需自定义请求模板。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/llms` | 分页查询 |
| GET | `/api/llms/{id}` | 查看详情 |
| POST | `/api/llms` | 创建 LLM |
| PUT | `/api/llms/{id}` | 更新 LLM |
| DELETE | `/api/llms/{id}` | 删除 LLM |

### 3. 题库管理

管理评测题目，支持单轮和多轮对话。提供 CRUD、批量删除、CSV/JSON 导入导出，以及 **AI 自动生成题目** 功能。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/questions` | 分页查询，支持 keyword/category/difficulty/questionType 筛选 |
| GET | `/api/questions/{id}` | 查看详情 |
| POST | `/api/questions` | 创建题目 |
| PUT | `/api/questions/{id}` | 更新题目 |
| DELETE | `/api/questions/{id}` | 删除题目 |
| DELETE | `/api/questions/batch` | 批量删除 |
| POST | `/api/questions/import` | 批量导入（CSV / JSON） |
| GET | `/api/questions/export` | 批量导出（CSV / JSON） |
| POST | `/api/questions/generate` | AI 生成题目 |

### 4. 评测任务

核心模块。创建评测任务时指定题目列表、参评 Agent/LLM 列表和评测维度（含权重和阈值），支持启动、重新开始、取消、进度轮询和结果查询。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/evaluation` | 分页查询任务列表 |
| GET | `/api/evaluation/{id}` | 查看任务详情 |
| POST | `/api/evaluation` | 创建评测任务 |
| PUT | `/api/evaluation/{id}` | 更新任务 |
| DELETE | `/api/evaluation/{id}` | 删除任务及关联结果 |
| POST | `/api/evaluation/{id}/start` | 启动评测（异步执行） |
| POST | `/api/evaluation/{id}/restart` | 重新开始（清空结果，保留配置） |
| POST | `/api/evaluation/{id}/cancel` | 取消评测 |
| GET | `/api/evaluation/{id}/progress` | 轮询进度（前端每 3s 调用） |
| GET | `/api/evaluation/{id}/results` | 查询评测结果（按 Agent 分组） |

### 评测引擎工作流

```
创建任务 → 选择题目 & Agent/LLM & 维度
         → 启动评测（异步线程池）
         → 遍历 agentIds × questionIds + llmIds × questionIds
         → 发送 HTTP 请求（单轮/多轮逐轮）
         → LLM-as-Judge 裁判打分
         → 写库（维度得分 + 综合分 + 通过判定）
         → 实时更新 completedCount
         → 前端轮询进度 → 完成
```

## 项目结构

```
agent-test-platform/
├── pom.xml                        # 父 POM — 多模块聚合 + 依赖版本管理
├── aun-common/                    # 公共层 — Response<T>、PageResult<T>、全局异常处理
├── aun-framework/                 # 框架层 — CORS、MyBatis-Plus、异步线程池配置
├── aun-agent/                     # Agent 模块 — Judge 裁判、题目生成
├── aun-web/                       # Web 业务层 — Controller / Service / Mapper / Entity
│   └── src/main/
│       ├── java/com/agenttest/
│       │   ├── controller/        # REST 接口
│       │   ├── service/           # 业务逻辑
│       │   ├── mapper/            # MyBatis-Plus Mapper
│       │   ├── engine/            # 评测引擎 & HTTP 客户端
│       │   └── pojo/
│       │       ├── entity/        # 数据库实体
│       │       ├── dto/           # 入参对象
│       │       └── vo/            # 出参对象（不含敏感字段）
│       └── resources/
│           ├── init.sql           # 数据库初始化脚本（含预置样本数据）
│           ├── application.yml    # 主配置
│           └── application-dev.yml# 开发环境配置
```

依赖链: `aun-common ← aun-framework ← aun-web`（aun-agent 模块独立，由 aun-web 调用）

## 数据库

| 表名 | 说明 |
|------|------|
| `agent` | 待评测 Agent |
| `llm` | 待评测 LLM 模型 |
| `question` | 题库（支持单轮/多轮） |
| `evaluation_task` | 评测任务（含 agentIds/llmIds/dimensions） |
| `evaluation_result` | 单题评测结果（含维度得分、响应内容） |
| `evaluation_report` | 评测报告 |

预置 25 条样本题目（20 单轮 + 5 多轮），覆盖推理/编程/问答/翻译/摘要五大分类。

## 部署指南

### 方式一：本地开发

```bash
# 1. 建库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS agent_aun CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
mysql -u root -p agent_aun < aun-web/src/main/resources/init.sql

# 2. 配置环境变量（或编辑 application-dev.yml）
export DB_HOST=localhost
export DB_PORT=3306
export DB_USERNAME=root
export DB_PASSWORD=your_password
export API_KEY=sk-xxx
export BASE_URL=https://api.deepseek.com
export MODEL=deepseek-chat

# 3. 编译 & 启动
mvn clean package -DskipTests
cd aun-web
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端启动在 http://localhost:8080。

### 方式二：手动构建镜像

```bash
# 构建后端镜像
cd AgentTest-all-u-need-java
docker build -t agent-test-backend .

# 运行（需先启动 MySQL）
docker run -d \
  -e DB_HOST=host.docker.internal \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=root123 \
  -e API_KEY=sk-xxx \
  -e BASE_URL=https://api.deepseek.com \
  -e MODEL=deepseek-chat \
  -p 8080:8080 \
  agent-test-backend
```

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| code | 含义 |
|------|------|
| 200  | 成功 |
| 400  | 参数校验失败 |
| 404  | 资源不存在 |
| 500  | 服务器内部错误 |

分页接口 `data` 内嵌 `PageResult`：`{ data: [...], total, page, pageSize }`。
