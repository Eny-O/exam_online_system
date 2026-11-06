package com.ai.exam_spring.controller;

import com.ai.exam_api.pojo.dto.QuestionBriefDTO;
import com.ai.exam_api.pojo.dto.StudentAnswerDTO;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.ai.exam_spring.modules.exam.pojo.dos.stu_answer.StudentAnswer;
import com.ai.exam_spring.modules.exam.service.IExamClassService;
import com.ai.exam_spring.modules.grade.service.IStudentAnswerService;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionCompleteVO;
import com.ai.exam_spring.modules.question.service.QuestionManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内部数据接口：跨服务只读数据查询
 */
@RestController
@RequestMapping("/inner")
public class InnerDataController {

    @Autowired
    private QuestionManagementService questionManagementService;

    @Autowired
    private IStudentAnswerService studentAnswerService;

    @Autowired
    private IExamClassService examClassService;

    @GetMapping("/question/batch")
    public ResponseResult<List<QuestionBriefDTO>> listQuestions(@RequestParam("ids") List<Integer> ids) {
        List<QuestionCompleteVO> questions = questionManagementService.getQuestionCompleteByIds(ids);
        List<QuestionBriefDTO> dtos = questions.stream().map(q -> {
            QuestionBriefDTO dto = new QuestionBriefDTO();
            dto.setId(q.getId());
            dto.setContent(q.getContent());
            dto.setType(q.getType() == null ? null : q.getType().name());
            dto.setKnowledgePoint(q.getKnowledgePoint());
            dto.setAnswer(q.getAnswer());
            dto.setAnalysis(q.getAnalysis());
            return dto;
        }).toList();
        return ResponseResult.success(dtos);
    }

    @GetMapping("/student-answer")
    public ResponseResult<List<StudentAnswerDTO>> listStudentAnswers(@RequestParam("examId") Integer examId,
                                                                     @RequestParam("studentId") Integer studentId) {
        List<StudentAnswer> answers = studentAnswerService.listByExamAndStudent(examId, studentId);
        List<StudentAnswerDTO> dtos = answers.stream().map(a -> {
            StudentAnswerDTO dto = new StudentAnswerDTO();
            dto.setId(a.getId());
            dto.setExamId(a.getExamId());
            dto.setStudentId(a.getStudentId());
            dto.setQuestionId(a.getQuestionId());
            dto.setStudentAnswer(a.getStudentAnswer());
            dto.setIsCorrect(a.getIsCorrect());
            dto.setScore(a.getScore());
            dto.setFullScore(a.getFullScore());
            dto.setTeacherComment(a.getTeacherComment());
            return dto;
        }).toList();
        return ResponseResult.success(dtos);
    }

    @GetMapping("/class/exam-refs")
    public ResponseResult<Boolean> hasClassReference(@RequestParam("classId") Integer classId) {
        return ResponseResult.success(examClassService.hasClassReference(classId));
    }
}
