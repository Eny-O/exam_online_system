package com.ai.exam_auth_user.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "编辑班级DTO")
public class ClassUpdateDTO {

    @NotNull(message = "班级ID不能为空")
    @Schema(description = "班级ID")
    private Integer id;

    @NotBlank(message = "班级名称不能为空")
    @Size(max = 50, message = "班级名称不能超过50个字符")
    @Schema(description = "班级名称")
    private String className;

    @Size(max = 20, message = "年级不能超过20个字符")
    @Schema(description = "年级")
    private String grade;
}
