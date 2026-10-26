package com.ai.exam_spring.modules.grade.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeMessageDTO implements Serializable {
    private Integer examId;
    private Integer studentId;
}