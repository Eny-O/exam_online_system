package com.ai.exam_spring.modules.grade.service.impl;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.BatchGradeDTO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.ExamAnswerVO;
import com.ai.exam_spring.modules.grade.mapper.GradePaperMapper;
import com.ai.exam_spring.modules.grade.mapper.StudentAnswerMapper;
import com.ai.exam_spring.modules.question.pojo.enums.QuestionTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GradePaperServiceImplTest {

    @Mock
    private GradePaperMapper gradePaperMapper;
    @Mock
    private StudentAnswerMapper studentAnswerMapper;
    @InjectMocks
    private GradePaperServiceImpl gradePaperService;

    private ExamAnswerVO answerOf(Integer id, String answer, String studentAnswer,
                                  Double fullScore, QuestionTypeEnum type) {
        ExamAnswerVO vo = new ExamAnswerVO();
        vo.setId(id);
        vo.setAnswer(answer);
        vo.setStudentAnswer(studentAnswer);
        vo.setFullScore(fullScore);
        vo.setType(type);
        return vo;
    }

    @Test
    @DisplayName("客观题答案正确 -> isCorrect=1 且得分=满分")
    void autoGrade_correctAnswer_scoresFull() {
        ExamAnswerVO vo = answerOf(1, "A", "A", 2.0, QuestionTypeEnum.SINGLE);
        when(gradePaperMapper.getExamAnswerDetail(10, 20)).thenReturn(List.of(vo));

        gradePaperService.autoGradeObjectiveQuestions(10, 20);

        ArgumentCaptor<List<BatchGradeDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentAnswerMapper).batchUpdateScore(captor.capture());
        List<BatchGradeDTO> graded = captor.getValue();
        assertEquals(1, graded.size());
        assertEquals(1, graded.get(0).getIsCorrect());
        assertEquals(2.0, graded.get(0).getScore());
    }

    @Test
    @DisplayName("客观题答案错误 -> isCorrect=0 且得分=0")
    void autoGrade_wrongAnswer_scoresZero() {
        ExamAnswerVO vo = answerOf(2, "A", "B", 2.0, QuestionTypeEnum.SINGLE);
        when(gradePaperMapper.getExamAnswerDetail(10, 20)).thenReturn(List.of(vo));

        gradePaperService.autoGradeObjectiveQuestions(10, 20);

        ArgumentCaptor<List<BatchGradeDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentAnswerMapper).batchUpdateScore(captor.capture());
        BatchGradeDTO dto = captor.getValue().get(0);
        assertEquals(0, dto.getIsCorrect());
        assertEquals(0.0, dto.getScore());
    }

    @Test
    @DisplayName("简答题(ESSAY)不评分，不参与批量更新")
    void autoGrade_essaySkipped() {
        ExamAnswerVO essay = answerOf(3, "参考答案", "学生作答", 10.0, QuestionTypeEnum.ESSAY);
        when(gradePaperMapper.getExamAnswerDetail(10, 20)).thenReturn(List.of(essay));

        gradePaperService.autoGradeObjectiveQuestions(10, 20);

        verify(studentAnswerMapper, never()).batchUpdateScore(anyList());
    }

    @Test
    @DisplayName("答案明细为空 -> 抛 EXAM_ANSWER_NOT_FOUND_NOT_AUTO_GRADE")
    void autoGrade_emptyAnswers_throws() {
        when(gradePaperMapper.getExamAnswerDetail(10, 20)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gradePaperService.autoGradeObjectiveQuestions(10, 20));
        assertEquals(ErrorCodeEnum.EXAM_ANSWER_NOT_FOUND_NOT_AUTO_GRADE.getCode(), ex.getErrorCode());
        verify(studentAnswerMapper, never()).batchUpdateScore(anyList());
    }

    @Test
    @DisplayName("混合题型：单选对+多选错+简答跳过 -> 仅2条非简答被评分")
    void autoGrade_mixedTypes() {
        ExamAnswerVO single = answerOf(1, "A", "A", 2.0, QuestionTypeEnum.SINGLE);
        ExamAnswerVO multiple = answerOf(2, "AB", "AC", 4.0, QuestionTypeEnum.MULTIPLE);
        ExamAnswerVO essay = answerOf(3, "要点", "学生要点", 10.0, QuestionTypeEnum.ESSAY);
        when(gradePaperMapper.getExamAnswerDetail(10, 20)).thenReturn(List.of(single, multiple, essay));

        gradePaperService.autoGradeObjectiveQuestions(10, 20);

        ArgumentCaptor<List<BatchGradeDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentAnswerMapper).batchUpdateScore(captor.capture());
        List<BatchGradeDTO> graded = captor.getValue();
        assertEquals(2, graded.size());
        BatchGradeDTO s = graded.stream().filter(d -> d.getId().equals(1)).findFirst().orElseThrow();
        assertEquals(1, s.getIsCorrect());
        assertEquals(2.0, s.getScore());
        BatchGradeDTO m = graded.stream().filter(d -> d.getId().equals(2)).findFirst().orElseThrow();
        assertEquals(0, m.getIsCorrect());
        assertEquals(0.0, m.getScore());
    }

}
