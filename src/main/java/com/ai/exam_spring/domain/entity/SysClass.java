package com.ai.exam_spring.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
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