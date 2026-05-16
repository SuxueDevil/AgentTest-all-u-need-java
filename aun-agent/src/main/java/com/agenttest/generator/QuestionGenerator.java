package com.agenttest.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 题库生成器 — 调用 LLM 自动生成评测题目。
 * <p>
 * 用户选择分类/难度/类型/数量，构造 Prompt 发给 DeepSeek，
 * 返回 JSON 数组经 Jackson 解析为 GeneratedQuestion 列表。
 */
@Component
public class QuestionGenerator {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerator.class);

    private final ChatClient chatClient;

    public QuestionGenerator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 批量生成题目。
     *
     * @param category     分类: reasoning/coding/qa/translation/summarization
     * @param difficulty   难度: easy/medium/hard
     * @param questionType 类型: single（单轮）/ multi（多轮）
     * @param count        生成数量 1~20
     * @return 生成的题目列表
     */
    @SuppressWarnings("unchecked")
    public List<GeneratedQuestion> generate(String category, String difficulty,
                                             String questionType, int count, String topic) {
        String prompt = buildPrompt(category, difficulty, questionType, count, topic);
        log.info("开始生成题目: category={} difficulty={} type={} count={}",
                category, difficulty, questionType, count);

        try {
            String content = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 提取 JSON 数组（LLM 可能在前后加 markdown 说明文字）
            int start = content.indexOf('[');
            int end = content.lastIndexOf(']') + 1;
            if (start >= 0 && end > start) {
                content = content.substring(start, end);
            }

            List<GeneratedQuestion> questions = new com.fasterxml.jackson.databind
                    .ObjectMapper().readValue(content,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
            log.info("题目生成完成: {} 道", questions.size());
            return questions;
        } catch (Exception e) {
            log.error("题目生成失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 构建生成 Prompt，包含分类中文名、难度、类型要求、可选主题场景。
     *
     * @param category     分类标识
     * @param difficulty   难度标识
     * @param questionType 题目类型: single / multi
     * @param count        生成数量
     * @param topic        主题场景（可为空），如"医疗问诊"
     * @return 完整的 Prompt 文本
     */
    private String buildPrompt(String category, String difficulty,
                                String questionType, int count, String topic) {
        String categoryName = switch (category != null ? category : "reasoning") {
            case "reasoning" -> "逻辑推理";
            case "coding" -> "编程";
            case "qa" -> "知识问答";
            case "translation" -> "翻译";
            case "summarization" -> "摘要";
            default -> "通用";
        };

        String difficultyName = switch (difficulty != null ? difficulty : "medium") {
            case "easy" -> "简单";
            case "medium" -> "中等";
            case "hard" -> "困难";
            default -> "中等";
        };

        String typeDesc = "single".equals(questionType)
                ? "单轮对话（只有一个 user 问题，不需要 assistant 回复）"
                : "多轮对话（包含 role=user 和 role=assistant 的交替 turns 数组）";

        String topicLine = topic != null && !topic.isBlank()
                ? "- 主题场景：%s\n".formatted(topic) : "";

        return """
               你是一个专业的 AI 评测题库生成器。请生成 %d 道%s类题目。

               要求：
               - 难度：%s
               - 类型：%s
               %s- 每题包含 title（题目标题）、expectedAnswer（期望答案/评分参考）、tags（标签数组，2-3个）

               %s

               严格按 JSON 数组格式返回，不要额外说明：
               [{"title":"…","expectedAnswer":"…","tags":["…","…"]}]
               """.formatted(
                count,
                categoryName,
                difficultyName,
                typeDesc,
                topicLine,
                "multi".equals(questionType)
                        ? "多轮题目的 turns 格式示例：\n  \"turns\":[{\"turnOrder\":1,\"role\":\"user\",\"content\":\"问题\"},{\"turnOrder\":2,\"role\":\"assistant\",\"content\":\"回答\"}]"
                        : ""
        );
    }

    /**
     * AI 生成的题目 DTO — 供 QuestionController 转换为 QuestionCreateDTO。
     */
    public record GeneratedQuestion(
            String title,
            String expectedAnswer,
            List<String> tags,
            List<Turn> turns
    ) {
        public record Turn(
                Integer turnOrder,
                String role,
                String content
        ) {}
    }
}
