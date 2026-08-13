package com.ai.exam_spring.service.exam.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.pojo.dto.stu_answer.StudentAnswerAddDTO;
import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.entity.exam.ExamStudent;
import com.ai.exam_spring.pojo.enums.exam.StudentExamEnum;
import com.ai.exam_spring.pojo.vo.exam.StuExamVO;
import com.ai.exam_spring.pojo.vo.paper.PaperContentVO;
import com.ai.exam_spring.pojo.vo.paper.StuPaperQuestionVO;
import com.ai.exam_spring.pojo.vo.paper.StuPaperVO;
import com.ai.exam_spring.pojo.vo.question.StuQuestionCompleteVO;
import com.ai.exam_spring.service.exam.IExamManagementService;
import com.ai.exam_spring.service.exam.IExamStudentService;
import com.ai.exam_spring.service.exam.StuExamService;
import com.ai.exam_spring.service.grading.IStudentAnswerService;
import com.ai.exam_spring.service.paper.IPaperManagementService;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class StuExamServiceImpl implements StuExamService {

    @Autowired
    private IExamManagementService examManagementService;

    @Autowired
    private IPaperManagementService paperManagementService;

    @Autowired
    private IStudentAnswerService studentAnswerService;

    @Autowired
    private IExamStudentService examStudentService;

    @Override
    public StuExamVO examInfoById(Integer id) {
        Exam exam = examManagementService.getById(id);
        if (exam == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        StuExamVO stuExamVO = new StuExamVO();
        BeanUtil.copyProperties(exam, stuExamVO);
        return stuExamVO;
    }

    /**
     * 根据id获取试卷信息
     * @param id
     * @return StuPaperVO
     */
    @Override
    public StuPaperVO getPaperById(Integer id) {
        PaperContentVO paperContentVO = paperManagementService.getPaperById(id);
        if (paperContentVO == null) {
            throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        }
        StuPaperVO stuPaperVO = new StuPaperVO();
        BeanUtil.copyProperties(paperContentVO, stuPaperVO);
        List<StuPaperQuestionVO> stuPaperQuestionVOS = paperContentVO.getPaperQuestionVOS().stream().map(paperQuestionVO -> {
            StuPaperQuestionVO stuPaperQuestionVO = new StuPaperQuestionVO();
            BeanUtil.copyProperties(paperQuestionVO, stuPaperQuestionVO);
            StuQuestionCompleteVO stuQuestionCompleteVO = new StuQuestionCompleteVO();
            BeanUtil.copyProperties(paperQuestionVO.getQuestionCompleteVO(), stuQuestionCompleteVO);
            stuPaperQuestionVO.setStuQuestionCompleteVO(stuQuestionCompleteVO);
            return stuPaperQuestionVO;
        }).toList();
        stuPaperVO.setStuPaperQuestionVOS(stuPaperQuestionVOS);
        return stuPaperVO;
    }

    /**
     * 提交试卷
     * @param studentAnswerAddDTO
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void submitPaper(StudentAnswerAddDTO studentAnswerAddDTO) {
        if (studentAnswerAddDTO == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        }
        ExamStudent dbExamStudent = examStudentService.getOne(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, studentAnswerAddDTO.getExamId())
                .eq(ExamStudent::getStudentId, Integer.parseInt(currentUserId)));
        if (dbExamStudent == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        if (!dbExamStudent.getStatus().equals(StudentExamEnum.PENDING)) {
            throw new BusinessException(ErrorCodeEnum.EXAM_STATUS_NOT_PENDING_NOT_SUBMIT);
        }
        Boolean isInsertSuccess = studentAnswerService.batchSaveStudentAnswers(studentAnswerAddDTO);
        if (isInsertSuccess) {
            ExamStudent examStudent = new ExamStudent();
            examStudent.setSubmitTime(LocalDateTime.now());
            examStudent.setStatus(StudentExamEnum.SUBMITTED);
            examStudentService.update(examStudent, new LambdaQueryWrapper<ExamStudent>()
                    .eq(ExamStudent::getExamId, studentAnswerAddDTO.getExamId())
                    .eq(ExamStudent::getStudentId, Integer.parseInt(currentUserId)));
        }else {
            throw new BusinessException(ErrorCodeEnum.STUDENT_SUBMIT_PAPER_FAILED);
        }
    }
}
