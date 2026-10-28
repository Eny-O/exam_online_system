package com.ai.exam_api.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 班级摘要（跨服务传输）
 * <p>
 * exam-core 拼装学生信息时需要班级名称与年级，通过 Feign 从 auth-user 拉取。
 */
@Data
@Schema(description = "班级摘要")
public class ClassBriefDTO {

    @Schema(description = "班级ID")
    private Integer id;

    @Schema(description = "班级名称")
    private String className;

    @Schema(description = "年级")
    private String grade;
}
