package com.ai.exam_spring.modules.user.pojo.dos;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 班级表
 * </p>
 *
 */
@TableName("sys_class")
@Data
public class SysClass {

    private Integer id; //

    private String className; // 班级名称

    private String grade; // 年级

    private LocalDateTime createTime; //

    private LocalDateTime updateTime; //

}