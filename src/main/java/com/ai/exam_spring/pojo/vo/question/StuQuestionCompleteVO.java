package com.ai.exam_spring.pojo.vo.question;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "学生题目展示对象")
public class StuQuestionCompleteVO {

    @Schema(description = "题目ID")
    private Integer id;

    @Schema(description = "题干内容")
    private String content;

    @Schema(
            description = "题目类型: single-单选 multiple-多选 judge-判断 essay-简答",
            allowableValues = {"single", "multiple", "judge", "essay"}
    )
    private String type;

    @Schema(description = "题目选项列表")
    private List<QuestionOptionVO> options;
}
