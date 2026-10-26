package com.ai.exam_spring.modules.exam.pojo.dto.stu_answer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量评分DTO，用于CASE WHEN批量更新
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchGradeDTO {
    private Integer id;
    private Integer isCorrect;
    private Double score;
}
