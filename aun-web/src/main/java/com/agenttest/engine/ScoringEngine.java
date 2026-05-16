package com.agenttest.engine;

import com.agenttest.pojo.entity.EvaluationResult.DimensionScore;
import com.agenttest.pojo.entity.EvaluationTask.DimensionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 评分引擎 — 根据评测维度对 Agent 响应进行打分。
 * <p>
 * 【当前为占位实现】Judge LLM 选型待定，暂用启发式规则打分：
 * <ul>
 *   <li>响应非空 +0.3 基础分</li>
 *   <li>响应长度 >100 字符 +0.3</li>
 *   <li>期望答案关键词命中率 +0.4</li>
 * </ul>
 * 后续替换为 LLM-as-Judge 时，只需修改此类的 score() 方法。
 */
@Component
public class ScoringEngine {

    private static final Logger log = LoggerFactory.getLogger(ScoringEngine.class);

    /**
     * 对单条响应进行多维度评分。
     *
     * @param responseContent Agent 返回的文本内容
     * @param expectedAnswer  期望答案（可能为 null）
     * @param dimensions      评测维度配置列表
     * @return 各维度得分明细
     */
    public List<DimensionScore> score(String responseContent, String expectedAnswer,
                                      List<DimensionConfig> dimensions) {
        List<DimensionScore> scores = new ArrayList<>();
        if (dimensions == null) return scores;

        for (DimensionConfig dim : dimensions) {
            double s = scoreDimension(responseContent, expectedAnswer, dim.getName());
            String feedback = generateFeedback(dim.getName(), s);
            DimensionScore ds = new DimensionScore();
            ds.setDimensionName(dim.getName());
            ds.setScore(Math.min(1.0, Math.max(0.0, s)));
            ds.setFeedback(feedback);
            scores.add(ds);
        }
        return scores;
    }

    /**
     * 计算综合加权得分。
     *
     * @param dimensionScores 各维度得分
     * @param dimensions      维度配置（含权重）
     * @return 加权综合得分 0~1
     */
    public double overallScore(List<DimensionScore> dimensionScores,
                               List<DimensionConfig> dimensions) {
        if (dimensionScores == null || dimensions == null || dimensions.isEmpty())
            return 0.0;

        double total = 0.0;
        double weightSum = 0.0;
        for (DimensionConfig dim : dimensions) {
            DimensionScore ds = findScore(dimensionScores, dim.getName());
            if (ds != null) {
                total += ds.getScore() * dim.getWeight();
                weightSum += dim.getWeight();
            }
        }
        return weightSum > 0 ? total / weightSum : 0.0;
    }

    /** 单维度打分 — 占位实现，后续替换为 LLM-as-Judge */
    private double scoreDimension(String content, String expected, String dimension) {
        if (content == null || content.isBlank()) return 0.0;
        double score = 0.3; // 响应非空基础分

        // 长度奖励
        if (content.length() > 100) score += 0.3;

        // 关键词命中
        if (expected != null && !expected.isBlank()) {
            long hits = countKeywordHits(content, expected);
            score += Math.min(0.4, hits * 0.08);
        }

        return score;
    }

    /** 关键词命中计数 — 将 expectedAnswer 按标点分词后在 content 中匹配 */
    private long countKeywordHits(String content, String expected) {
        String[] keywords = expected.split("[，,。.；;！!？?\\s]+");
        long hits = 0;
        for (String kw : keywords) {
            if (kw.length() >= 2 && content.contains(kw)) hits++;
        }
        return hits;
    }

    private String generateFeedback(String dimension, double score) {
        if (score >= 0.8) return "表现优秀";
        if (score >= 0.6) return "表现良好";
        if (score >= 0.4) return "表现一般";
        return "表现较差";
    }

    private DimensionScore findScore(List<DimensionScore> scores, String name) {
        return scores.stream()
                .filter(s -> s.getDimensionName().equals(name))
                .findFirst().orElse(null);
    }
}
