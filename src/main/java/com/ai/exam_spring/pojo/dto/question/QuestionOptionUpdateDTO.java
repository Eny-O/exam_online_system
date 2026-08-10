package com.ai.exam_spring.pojo.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "题目选项更新数据传输对象")
public class QuestionOptionUpdateDTO extends BaseQuestionOptionDTO{

    @Schema(description = "选项id")
    private Integer id;


}
