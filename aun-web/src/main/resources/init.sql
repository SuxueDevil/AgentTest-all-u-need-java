-- ============================================================
-- Agent 智能评测平台 — 数据库初始化（轻量版 5 张表）
-- MySQL 8.0
-- ============================================================

CREATE DATABASE IF NOT EXISTS agent_aun
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE agent_aun;

-- ============================================================
-- 1. agent — 待评测 Agent
-- ============================================================
CREATE TABLE IF NOT EXISTS agent (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL COMMENT 'Agent名称',
    description     VARCHAR(500) COMMENT '描述',
    model           VARCHAR(100) COMMENT '模型标识',
    type            VARCHAR(30) NOT NULL DEFAULT 'llm' COMMENT '类型',
    status          VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态',
    endpoint_url    VARCHAR(500) COMMENT 'API端点',
    auth_type       VARCHAR(20) DEFAULT 'none' COMMENT '鉴权方式: none/bearer/api_key/basic',
    auth_credential VARCHAR(1000) COMMENT '鉴权凭证',
    tags            JSON COMMENT '标签',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent';

-- ============================================================
-- 2. question — 题库（多轮对话以 JSON 存储）
-- ============================================================
CREATE TABLE IF NOT EXISTS question (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(300) NOT NULL COMMENT '问题标题',
    category        VARCHAR(30) NOT NULL COMMENT '分类: reasoning/coding/qa/translation/summarization',
    difficulty      VARCHAR(10) NOT NULL DEFAULT 'medium' COMMENT '难度: easy/medium/hard',
    question_type   VARCHAR(10) NOT NULL DEFAULT 'single' COMMENT 'single 单轮 / multi 多轮',
    turns           JSON COMMENT '多轮对话内容 [{turn_order, role, content}]',
    expected_answer TEXT COMMENT '期望答案（评分参考）',
    tags            JSON COMMENT '标签',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库';

-- ============================================================
-- 3. evaluation_task — 评测任务（内嵌 agent_ids、维度配置）
-- ============================================================
CREATE TABLE IF NOT EXISTS evaluation_task (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL COMMENT '任务名称',
    description     VARCHAR(500) COMMENT '描述',
    question_ids    JSON NOT NULL COMMENT '题目ID列表',
    agent_ids       JSON NOT NULL COMMENT '参评Agent ID列表',
    dimensions      JSON NOT NULL COMMENT '评测维度配置 [{name, displayName, weight, threshold}]',
    question_count  INT NOT NULL DEFAULT 0 COMMENT '题目总数',
    completed_count INT NOT NULL DEFAULT 0 COMMENT '已完成数',
    status          VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/completed/cancelled/failed',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at      DATETIME COMMENT '开始时间',
    completed_at    DATETIME COMMENT '完成时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评测任务';

-- ============================================================
-- 4. evaluation_result — 单题评测结果（内嵌维度明细）
-- ============================================================
CREATE TABLE IF NOT EXISTS evaluation_result (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT NOT NULL COMMENT '任务ID',
    agent_id        BIGINT NOT NULL COMMENT 'Agent ID',
    question_id     BIGINT NOT NULL COMMENT '问题ID',
    overall_score   DECIMAL(5,3) COMMENT '综合加权得分(0-1)',
    passed          BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否通过',
    latency_ms      INT COMMENT '响应延迟',
    tokens_used     INT COMMENT '消耗token数',
    dimension_scores JSON COMMENT '维度得分明细 [{dimensionName, score, feedback}]',
    raw_request     JSON COMMENT '原始请求',
    raw_response    TEXT COMMENT 'Agent原始响应',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES evaluation_task(id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agent(id),
    FOREIGN KEY (question_id) REFERENCES question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评测结果';

-- ============================================================
-- 5. evaluation_report — 评测报告
-- ============================================================
CREATE TABLE IF NOT EXISTS evaluation_report (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT NOT NULL COMMENT '任务ID',
    title           VARCHAR(200) NOT NULL COMMENT '报告标题',
    summary         TEXT COMMENT '文字总结',
    chart_data      JSON COMMENT '图表序列化数据',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES evaluation_task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评测报告';
