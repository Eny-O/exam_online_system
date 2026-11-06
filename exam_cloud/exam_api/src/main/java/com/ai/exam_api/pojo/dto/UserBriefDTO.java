package com.ai.exam_api.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户摘要（跨服务传输）
 * <p>
 * exam-core 在拼装考试详情、批阅名单时需要用户的少量档案字段，
 * 通过 Feign 从 auth-user 拉取，不直接依赖 auth-user 的实体类。
 */
@Data
@Schema(description = "用户摘要")
public class UserBriefDTO {

    @Schema(description = "用户ID")
    private Integer id;

    @Schema(description = "学号（学生）")
    private String studentNo;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "学生归属班级ID")
    private Integer classId;
}
