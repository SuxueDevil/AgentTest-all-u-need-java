-- ============================================================
-- Agent 智能评测平台 — 数据库初始化（轻量版 5 张表）
-- MySQL 8.0
-- ============================================================

CREATE DATABASE IF NOT EXISTS agent_aun
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE agent_aun;

-- ============================================================
-- 1. llm — 待评测 LLM 模型
-- ============================================================
CREATE TABLE IF NOT EXISTS llm (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL COMMENT 'LLM名称',
    model           VARCHAR(100) NOT NULL COMMENT '模型标识，如deepseek-chat',
    endpoint_url    VARCHAR(500) COMMENT 'API端点',
    api_key         VARCHAR(1000) COMMENT 'API Key',
    status          VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM模型';

-- ============================================================
-- 2. agent — 待评测 Agent 应用
-- ============================================================
CREATE TABLE IF NOT EXISTS agent (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL COMMENT 'Agent名称',
    description     VARCHAR(500) COMMENT '描述',
    model           VARCHAR(100) COMMENT '模型标识',
    type            VARCHAR(30) NOT NULL DEFAULT 'llm' COMMENT '类型',
    status          VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态',
    endpoint_url    VARCHAR(500) COMMENT 'API端点',
    request_body    TEXT COMMENT '请求模板JSON，{{messages}}占位',
    response_protocol VARCHAR(10) DEFAULT 'auto' COMMENT '响应协议: sse/json/auto',
    response_content_path VARCHAR(100) COMMENT '响应提取路径，如choices[0].message.content',
    auth_type       VARCHAR(20) DEFAULT 'none' COMMENT '鉴权方式: none/bearer/api_key/basic/custom',
    auth_credential VARCHAR(1000) COMMENT '鉴权凭证',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
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
    turns           JSON COMMENT '多轮对话内容 [{turnOrder, role, content}]',
    expected_answer TEXT COMMENT '期望答案（评分参考）',
    tags            JSON COMMENT '标签',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删 1=已删',
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
    agent_ids       JSON COMMENT '参评Agent ID列表',
    llm_ids         JSON COMMENT '参评LLM ID列表',
    dimensions      JSON NOT NULL COMMENT '评测维度配置 [{name, displayName, weight, threshold}]',
    question_count  INT NOT NULL DEFAULT 0 COMMENT '题目总数',
    completed_count INT NOT NULL DEFAULT 0 COMMENT '已完成数',
    run             INT NOT NULL DEFAULT 1 COMMENT '评测批次号，每次重新开始自增',
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
    agent_id        BIGINT COMMENT 'Agent ID',
    llm_id          BIGINT COMMENT 'LLM ID',
    question_id     BIGINT NOT NULL COMMENT '问题ID',
    run             INT NOT NULL DEFAULT 1 COMMENT '评测批次号',
    overall_score   DECIMAL(5,3) COMMENT '综合加权得分(0-1)',
    passed          BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否通过',
    latency_ms      INT COMMENT '响应延迟',
    tokens_used     INT COMMENT '消耗token数',
    dimension_scores JSON COMMENT '维度得分明细 [{dimensionName, score, feedback}]',
    raw_request     JSON COMMENT '原始请求',
    raw_response    TEXT COMMENT 'Agent原始响应',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES evaluation_task(id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE,
    FOREIGN KEY (llm_id) REFERENCES llm(id) ON DELETE CASCADE,
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

-- ============================================================
-- 预置样本数据 — 20 条单轮 + 5 条多轮
-- ============================================================

-- 推理类（reasoning）— 6 条（4 单轮 + 2 多轮）
INSERT INTO question (title, category, difficulty, question_type, turns, expected_answer, tags) VALUES
('三段论推理：所有A是B，所有B是C，那么A和C的关系是什么？', 'reasoning', 'easy', 'single', NULL, '所有A是C，这是典型的三段论推理结论', '["逻辑","三段论"]'),
('分析：如果全球气温上升3°C，可能产生哪些连锁反应？', 'reasoning', 'medium', 'multi',
 '[{"turnOrder":1,"role":"user","content":"如果全球气温上升3°C，沿海城市会怎样？"},{"turnOrder":2,"role":"assistant","content":"海平面可能上升约0.5-1米，沿海城市面临洪涝风险。"},{"turnOrder":3,"role":"user","content":"那对农业呢？"}]',
 '应分析海平面上升、极端天气、粮食减产、生态破坏等连锁反应', '["气候","推理"]'),
('为什么1+1=2？请从数学基础角度解释', 'reasoning', 'hard', 'single', NULL, '皮亚诺公理定义自然数，1是S(0)，2是S(S(0))，1+1=2来源于加法定义', '["数学","基础"]'),
('论证题：科技发展是否会加剧社会不平等？', 'reasoning', 'medium', 'single', NULL, '从技术获取成本、教育差距、就业替代等角度辩证分析', '["社会","辩证"]'),
('分析这句话的逻辑谬误："因为他有名，所以他说的是对的"', 'reasoning', 'easy', 'single', NULL, '诉诸权威的谬误：名人的知名度与其言论的正确性无关', '["逻辑谬误","批判思维"]'),
('多轮归因分析：某电商平台GMV下滑20%，请逐层分析原因', 'reasoning', 'hard', 'multi',
 '[{"turnOrder":1,"role":"user","content":"我们的GMV下滑20%，先看是不是流量问题？"},{"turnOrder":2,"role":"assistant","content":"先拉流量数据对比，DAU和曝光量是关键指标。"},{"turnOrder":3,"role":"user","content":"DAU降了5%，但转化率从3%跌到1.8%，这才是主因吧？"},{"turnOrder":4,"role":"assistant","content":"对，转化率下跌比流量下跌更致命，需要排查落地页、商品详情、支付流程。"}]',
 '归因分析应从流量、转化率、客单价逐层拆解，定位关键瓶颈', '["商业分析","归因"]');

-- 编程类（coding）— 6 条（4 单轮 + 2 多轮）
INSERT INTO question (title, category, difficulty, question_type, turns, expected_answer, tags) VALUES
('用Python实现二分查找算法', 'coding', 'easy', 'single', NULL, '实现左右指针，计算mid，根据比较结果移动指针，时间复杂度O(log n)', '["算法","Python","二分查找"]'),
('什么是RESTful API？简述其设计原则', 'coding', 'easy', 'single', NULL, '资源导向、使用HTTP方法语义、无状态、统一接口、HATEOAS', '["API","REST","设计"]'),
('优化查询：数据库中有一个百万级用户表，需要按姓名模糊搜索并排序，如何设计索引？', 'coding', 'medium', 'single', NULL, '使用全文索引或倒排索引，B+树对模糊前缀有效，like "%xx%"走全表扫描需考虑ES', '["数据库","索引","优化"]'),
('实现一个支持过期时间的LRU缓存', 'coding', 'hard', 'single', NULL, '哈希表+双向链表+过期时间戳，get时检查过期，put时淘汰最近最少使用且未过期的', '["数据结构","缓存","LRU"]'),
('多轮代码调试：这段代码为什么死锁？', 'coding', 'hard', 'multi',
 '[{"turnOrder":1,"role":"user","content":"线程A持有lock1等待lock2，线程B持有lock2等待lock1，为什么死锁了？"},{"turnOrder":2,"role":"assistant","content":"这是经典的循环等待，两个线程互相持有对方需要的锁。"},{"turnOrder":3,"role":"user","content":"怎么解决？改成tryLock超时行吗？"}]',
 '分析死锁四个条件（互斥、持有等待、不可剥夺、循环等待），并给出破坏其中任一条件的方案', '["并发","死锁","Java"]'),
('设计一个短链接系统，支持高并发访问', 'coding', 'medium', 'multi',
 '[{"turnOrder":1,"role":"user","content":"短链接系统的核心是什么？"},{"turnOrder":2,"role":"assistant","content":"核心是发号器+base62编码，还要考虑缓存和跳转。"},{"turnOrder":3,"role":"user","content":"发号器怎么做？分布式ID生成？"}]',
 '发号器可用雪花算法或自增ID，短码=base62(ID)，缓存存储映射关系，跳转做301重定向', '["系统设计","短链接","分布式"]');

-- 问答类（qa）— 5 条（4 单轮 + 1 多轮）
INSERT INTO question (title, category, difficulty, question_type, turns, expected_answer, tags) VALUES
('什么是机器学习中的过拟合？如何防止？', 'qa', 'medium', 'single', NULL, '模型在训练集表现好但测试集差，防止方法：正则化、dropout、早停、增加数据、降维', '["机器学习","过拟合"]'),
('Git中rebase和merge的区别是什么？', 'qa', 'easy', 'single', NULL, 'merge保留分支历史并创建合并提交，rebase将提交移到目标分支顶端形成线性历史', '["Git","版本控制"]'),
('Transformer的注意力机制是如何工作的？', 'qa', 'hard', 'single', NULL, 'QK^T计算相似度，除以√dk缩放，softmax归一化得到权重，加权求和V得到输出', '["深度学习","Transformer","注意力"]'),
('Docker容器和虚拟机的核心区别？', 'qa', 'easy', 'single', NULL, '容器共享宿主机内核，启动快、资源少；虚拟机有完整OS，隔离强但开销大', '["Docker","虚拟化"]'),
('多轮技术答疑：微服务拆分后，如何处理分布式事务？', 'qa', 'hard', 'multi',
 '[{"turnOrder":1,"role":"user","content":"我们把单体拆成微服务后，跨服务的事务怎么处理？"},{"turnOrder":2,"role":"assistant","content":"主要有几种方案：Saga、TCC、本地消息表+MQ、Seata。"},{"turnOrder":3,"role":"user","content":"Saga和TCC的区别是什么？选哪个？"}]',
 'Saga适合长事务，用补偿回滚，最终一致性；TCC分Try-Confirm-Cancel三阶段，适合强一致性场景', '["微服务","分布式事务","Saga"]');

-- 翻译类（translation）— 3 条（全单轮）
INSERT INTO question (title, category, difficulty, question_type, turns, expected_answer, tags) VALUES
('将以下技术文档翻译成中文："The API gateway acts as a single entry point for all client requests, handling authentication, rate limiting, and request routing."', 'translation', 'medium', 'single', NULL, 'API网关作为所有客户端请求的单一入口，负责处理认证、限流和请求路由。', '["技术文档","英译中"]'),
('将"春江潮水连海平，海上明月共潮生"翻译成英文', 'translation', 'hard', 'single', NULL, 'In spring the river rises as high as the sea, and with the rivers rise to the moon upborne bright.', '["诗歌","中译英"]'),
('将以下产品介绍翻译成英文："我们的产品采用AI驱动的自然语言处理技术，支持12种语言的实时翻译，延迟低于200毫秒。"', 'translation', 'easy', 'single', NULL, 'Our product uses AI-driven NLP technology, supporting real-time translation in 12 languages with latency under 200ms.', '["产品介绍","中译英"]');

-- 摘要类（summarization）— 3 条（全单轮）
INSERT INTO question (title, category, difficulty, question_type, turns, expected_answer, tags) VALUES
('请用一段话总结以下技术演讲稿的核心内容：演讲者分享了他们在从单体架构迁移到微服务过程中的经验教训，包括数据拆分策略、服务边界划分、以及团队结构如何与技术架构对齐。', 'summarization', 'medium', 'single', NULL, '演讲者分享了单体到微服务迁移的三大要点：数据拆分、服务边界划分和团队对齐，强调组织架构应匹配技术架构。', '["技术演讲","微服务"]'),
('用三句话总结这篇3000字的气候变化研究报告', 'summarization', 'hard', 'single', NULL, '概括研究背景和核心发现、关键数据支持、实践建议', '["研究","气候变化"]'),
('用一句话总结产品需求文档的要点', 'summarization', 'easy', 'single', NULL, '产品需求文档定义了产品的目标用户、核心功能、验收标准和时间节点。', '["PRD","产品"]');
