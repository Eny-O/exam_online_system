package com.ai.exam_api.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "学生信息展示对象")
public class UStudentVO {
    @Schema(description = "学生id", example = "1")
    private Integer id;

    @Schema(description = "学号", example = "1001")
    public String studentNo;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "班级名称", example = "软件工程1班")
    private String className;

    @Schema(description = "专业", example = "软件工程")
    private String major;

    @Schema(description = "年级", example = "2023级")
    private String grade;
}
