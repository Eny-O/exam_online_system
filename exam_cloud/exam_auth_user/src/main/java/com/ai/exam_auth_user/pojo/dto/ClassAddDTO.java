package com.ai.exam_auth_user.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新增班级DTO")
public class ClassAddDTO {

    @NotBlank(message = "班级名称不能为空")
    @Size(max = 50, message = "班级名称不能超过50个字符")
    @Schema(description = "班级名称")
    private String className;

    @Size(max = 20, message = "年级不能超过20个字符")
    @Schema(description = "年级")
    private String grade;
}
