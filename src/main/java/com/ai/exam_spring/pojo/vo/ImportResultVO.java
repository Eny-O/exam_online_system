package com.ai.exam_spring.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
/**
 * 导入结果响应对象
 */
@Data
@Schema(description = "导入结果响应对象")
public class ImportResultVO {
    @Schema(description = "成功导入的题目数量", example = "10")
    private int successCount;
    @Schema(description = "导入失败的题目数量", example = "2")
    private int failCount;
}
