package com.ai.exam_auth_user.pojo.dos;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id; //

    private String className; // 班级名称

    private String grade; // 年级

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime; //

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime; //

    private Integer deleted; // 0-启用；停用后写为该行id

    private String remark; // 备注

}