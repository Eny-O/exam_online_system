package com.ai.exam_auth_user.pojo.dos;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-班级关联表
 */
@Data
@TableName("sys_user_class")
public class UserClass {

    /** 用户ID */
    private Integer userId;

    /** 班级ID */
    private Integer classId;

    /** 关系类型: 1-学生归属, 2-教师任教 */
    private Integer relationType;

    @TableField(fill = FieldFill.INSERT)
    /** 创建时间 */
    private LocalDateTime createTime;
}
