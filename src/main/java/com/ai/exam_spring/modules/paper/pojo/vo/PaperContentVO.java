package com.ai.exam_spring.modules.paper.pojo.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "试卷内容展示对象")
public class PaperContentVO extends BasePaperVO {

    @Schema(description = "试卷题目列表")
    private List<PaperQuestionVO> paperQuestionVOS;
}
