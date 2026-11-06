package com.ai.exam_auth_user.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysClassVO {

    private Integer id; //

    private String className; // 班级名称

    private String grade; // 年级

    private LocalDateTime createTime; // 创建时间
}
