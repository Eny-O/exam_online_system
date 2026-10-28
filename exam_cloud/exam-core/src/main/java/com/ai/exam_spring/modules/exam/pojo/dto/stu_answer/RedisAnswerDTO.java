package com.ai.exam_spring.modules.exam.pojo.dto.stu_answer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Redis暂存答案DTO
 * 学生答题时暂存到Redis，提交时批量写入数据库
 */
@Data
@Schema(description = "Redis暂存答案数据")
public class RedisAnswerDTO {

    @Schema(description = "学生作答内容", example = "A")
    private String studentAnswer;

    @Schema(description = "满分", example = "2.0")
    private Double fullScore;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
