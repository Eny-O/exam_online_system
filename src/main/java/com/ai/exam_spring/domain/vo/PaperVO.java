package com.ai.exam_spring.domain.vo;

import com.ai.exam_spring.common.status.PaperStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;

import java.time.LocalDateTime;

@Data
@Schema(description = "试卷信息展示对象")
public class PaperVO extends BasePaperVO{

    @Schema(description = "创建教师姓名")
    private String teacherName;

    @Schema(description = "试卷状态:0为草稿,1为待启用,2为已启用")
    private PaperStatus status;


    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
