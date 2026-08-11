package com.ai.exam_spring.service.exam;

import com.ai.exam_spring.pojo.vo.exam.StuExamVO;
import com.ai.exam_spring.pojo.vo.paper.StuPaperVO;

public interface StuExamService {
    /**
     * 根据id获取考试信息
     * @param id
     * @return
     */
    StuExamVO examInfoById(Integer id);

    /**
     * 根据id获取试卷信息
     * @param id
     * @return
     */
    StuPaperVO getPaperById(Integer id);
}
