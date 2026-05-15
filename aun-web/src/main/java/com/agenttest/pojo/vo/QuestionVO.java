package com.agenttest.pojo.vo;

import com.agenttest.pojo.entity.Question;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问题视图对象 — Controller 返回给前端的问题数据。
 * <p>
 * 与 entity 字段基本一致（问题数据无敏感字段），
 * 由 Service 层通过 BeanUtil.copyProperties 从 entity 转换。
 */
@Data
public class QuestionVO {

    /** 主键 ID */
    private Long id;
    /** 问题标题 */
    private String title;
    /** 分类: reasoning / coding / qa / translation / summarization */
    private String category;
    /** 难度: easy / medium / hard */
    private String difficulty;
    /** 问题类型: single（单轮）/ multi（多轮） */
    private String questionType;
    /** 多轮对话内容，单轮时为空或单元素数组 */
    private List<Question.Turn> turns;
    /** 期望答案（评分参考） */
    private String expectedAnswer;
    /** 标签列表 */
    private List<String> tags;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
