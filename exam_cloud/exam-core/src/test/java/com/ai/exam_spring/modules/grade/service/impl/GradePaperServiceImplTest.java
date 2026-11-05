package com.ai.exam_spring.modules.grade.service.impl;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamPaperSnapshotDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.BatchGradeDTO;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.ExamAnswerVO;
import com.ai.exam_spring.modules.exam.service.IExamPaperSnapshotService;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.grade.mapper.GradePaperMapper;
import com.ai.exam_spring.modules.grade.mapper.StudentAnswerMapper;
import com.ai.exam_spring.modules.grade.pojo.vo.GradePaperContentVO;
import com.ai.exam_spring.modules.grade.pojo.vo.GradeStudentVO;
import com.ai.exam_spring.modules.grade.pojo.vo.StudentScoreSummaryVO;
import com.ai.exam_api.feign.UserFeignClient;
import com.ai.exam_api.pojo.dto.UserBriefDTO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradePaperServiceImplTest {

    @Mock
    private GradePaperMapper gradePaperMapper;
    @Mock
    private StudentAnswerMapper studentAnswerMapper;
    @Mock
    private IExamPaperSnapshotService examPaperSnapshotService;
    @Mock
    private IExamStudentService examStudentService;
    @Mock
    private UserFeignClient userFeignClient;

    @InjectMocks
    private GradePaperServiceImpl gradePaperService;

    private static final Integer EXAM_ID = 10;
    private static final Integer STUDENT_ID = 20;

    private ExamAnswerVO answerOf(Integer id, Integer questionId, String type,
                                  String studentAnswer, Double fullScore) {
        ExamAnswerVO vo = new ExamAnswerVO();
        vo.setId(id);
        vo.setQuestionId(questionId);
        vo.setType(type);
        vo.setStudentAnswer(studentAnswer);
        vo.setFullScore(fullScore);
        return vo;
    }

    private ExamPaperSnapshotDTO.QuestionInfo questionOf(Integer questionId, String answer) {
        ExamPaperSnapshotDTO.QuestionInfo question = new ExamPaperSnapshotDTO.QuestionInfo();
        question.setQuestionId(questionId);
        question.setAnswer(answer);
        return question;
    }

    private ExamPaperSnapshotDTO snapshotOf(ExamPaperSnapshotDTO.QuestionInfo... questions) {
        ExamPaperSnapshotDTO dto = new ExamPaperSnapshotDTO();
        dto.setQuestions(List.of(questions));
        return dto;
    }

    private ExamStudent examStudentOf(Integer studentId, StudentExamEnum status) {
        ExamStudent examStudent = new ExamStudent();
        examStudent.setExamId(EXAM_ID);
        examStudent.setStudentId(studentId);
        examStudent.setStatus(status);
        return examStudent;
    }

    private StudentScoreSummaryVO summaryOf(Integer studentId,
                                            Double objective, Double subjective, Double total) {
        StudentScoreSummaryVO summary = new StudentScoreSummaryVO();
        summary.setStudentId(studentId);
        summary.setObjectiveScore(objective);
        summary.setSubjectiveScore(subjective);
        summary.setTotalScore(total);
        return summary;
    }

    private UserBriefDTO userOf(Integer id, String realName, String studentNo) {
        UserBriefDTO user = new UserBriefDTO();
        user.setId(id);
        user.setRealName(realName);
        user.setStudentNo(studentNo);
        return user;
    }

    private List<BatchGradeDTO> captureGraded() {
        ArgumentCaptor<List<BatchGradeDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentAnswerMapper).batchUpdateScore(captor.capture());
        return captor.getValue();
    }

    // ===================== getGradePaper =====================

    @Test
    @DisplayName("getGradePaper 入参为null -> 抛 PARAM_ERROR 且不读快照")
    void getGradePaper_nullParam() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> gradePaperService.getGradePaper(null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verifyNoInteractions(examPaperSnapshotService);
    }

    @Test
    @DisplayName("getGradePaper 正常 -> 读快照并转换为批阅卷面")
    void getGradePaper_usesSnapshot() {
        ExamPaperSnapshotDTO dto = new ExamPaperSnapshotDTO();
        GradePaperContentVO vo = new GradePaperContentVO();
        vo.setName("期中试卷");
        vo.setTotalScore(100.0);
        when(examPaperSnapshotService.getRequired(EXAM_ID)).thenReturn(dto);
        when(examPaperSnapshotService.toGradePaper(dto)).thenReturn(vo);

        GradePaperContentVO result = gradePaperService.getGradePaper(EXAM_ID);

        assertSame(vo, result);
        assertEquals("期中试卷", result.getName());
        verify(examPaperSnapshotService).getRequired(EXAM_ID);
        verify(examPaperSnapshotService).toGradePaper(dto);
    }

    @Test
    @DisplayName("getGradePaper 快照不存在 -> 透传 EXAM_SNAPSHOT_NOT_FOUND")
    void getGradePaper_snapshotMissing() {
        when(examPaperSnapshotService.getRequired(EXAM_ID))
                .thenThrow(new BusinessException(ErrorCodeEnum.EXAM_SNAPSHOT_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gradePaperService.getGradePaper(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_SNAPSHOT_NOT_FOUND.getCode(), ex.getErrorCode());
        verify(examPaperSnapshotService, never()).toGradePaper(any(ExamPaperSnapshotDTO.class));
    }

    // ===================== autoGradeObjectiveQuestions =====================

    @Test
    @DisplayName("自动批阅 答案明细为空 -> 抛 EXAM_ANSWER_NOT_FOUND_NOT_AUTO_GRADE")
    void autoGrade_emptyAnswers_throws() {
        when(gradePaperMapper.getExamAnswerDetail(EXAM_ID, STUDENT_ID)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gradePaperService.autoGradeObjectiveQuestions(EXAM_ID, STUDENT_ID));
        assertEquals(ErrorCodeEnum.EXAM_ANSWER_NOT_FOUND_NOT_AUTO_GRADE.getCode(), ex.getErrorCode());
        verify(studentAnswerMapper, never()).batchUpdateScore(anyList());
        verifyNoInteractions(examPaperSnapshotService);
    }

    @Test
    @DisplayName("自动批阅 答案正确 -> isCorrect=1 且得分=满分")
    void autoGrade_correctAnswer_scoresFull() {
        when(gradePaperMapper.getExamAnswerDetail(EXAM_ID, STUDENT_ID))
                .thenReturn(List.of(answerOf(1, 100, "single", "A", 2.0)));
        when(examPaperSnapshotService.getRequired(EXAM_ID))
                .thenReturn(snapshotOf(questionOf(100, "A")));

        gradePaperService.autoGradeObjectiveQuestions(EXAM_ID, STUDENT_ID);

        List<BatchGradeDTO> graded = captureGraded();
        assertEquals(1, graded.size());
        assertEquals(1, graded.get(0).getId().intValue());
        assertEquals(1, graded.get(0).getIsCorrect().intValue());
        assertEquals(2.0, graded.get(0).getScore(), 1e-9);
    }

    @Test
    @DisplayName("自动批阅 答案错误 -> isCorrect=0 且得分=0")
    void autoGrade_wrongAnswer_scoresZero() {
        when(gradePaperMapper.getExamAnswerDetail(EXAM_ID, STUDENT_ID))
                .thenReturn(List.of(answerOf(2, 100, "single", "B", 2.0)));
        when(examPaperSnapshotService.getRequired(EXAM_ID))
                .thenReturn(snapshotOf(questionOf(100, "A")));

        gradePaperService.autoGradeObjectiveQuestions(EXAM_ID, STUDENT_ID);

        List<BatchGradeDTO> graded = captureGraded();
        assertEquals(1, graded.size());
        assertEquals(0, graded.get(0).getIsCorrect().intValue());
        assertEquals(0.0, graded.get(0).getScore(), 1e-9);
    }

    @Test
    @DisplayName("自动批阅 满分为null -> 判对但得分按0处理")
    void autoGrade_nullFullScore_scoresZero() {
        when(gradePaperMapper.getExamAnswerDetail(EXAM_ID, STUDENT_ID))
                .thenReturn(List.of(answerOf(5, 100, "single", "A", null)));
        when(examPaperSnapshotService.getRequired(EXAM_ID))
                .thenReturn(snapshotOf(questionOf(100, "A")));

        gradePaperService.autoGradeObjectiveQuestions(EXAM_ID, STUDENT_ID);

        List<BatchGradeDTO> graded = captureGraded();
        assertEquals(1, graded.get(0).getIsCorrect().intValue());
        assertEquals(0.0, graded.get(0).getScore(), 1e-9);
    }

    @Test
    @DisplayName("自动批阅 简答题跳过 -> 不批量更新")
    void autoGrade_essaySkipped() {
        when(gradePaperMapper.getExamAnswerDetail(EXAM_ID, STUDENT_ID))
                .thenReturn(List.of(answerOf(3, 100, "essay", "学生作答", 10.0)));
        when(examPaperSnapshotService.getRequired(EXAM_ID))
                .thenReturn(snapshotOf(questionOf(100, "要点")));

        gradePaperService.autoGradeObjectiveQuestions(EXAM_ID, STUDENT_ID);

        verify(studentAnswerMapper, never()).batchUpdateScore(anyList());
    }

    @Test
    @DisplayName("自动批阅 题目不在快照 -> 跳过，不批量更新")
    void autoGrade_questionNotInSnapshot_skipped() {
        when(gradePaperMapper.getExamAnswerDetail(EXAM_ID, STUDENT_ID))
                .thenReturn(List.of(answerOf(4, 100, "single", "A", 2.0)));
        when(examPaperSnapshotService.getRequired(EXAM_ID))
                .thenReturn(snapshotOf(questionOf(999, "A")));

        gradePaperService.autoGradeObjectiveQuestions(EXAM_ID, STUDENT_ID);

        verify(studentAnswerMapper, never()).batchUpdateScore(anyList());
    }

    @Test
    @DisplayName("自动批阅 快照题目列表为null -> 跳过，不批量更新")
    void autoGrade_nullQuestions_skipped() {
        when(gradePaperMapper.getExamAnswerDetail(EXAM_ID, STUDENT_ID))
                .thenReturn(List.of(answerOf(1, 100, "single", "A", 2.0)));
        when(examPaperSnapshotService.getRequired(EXAM_ID))
                .thenReturn(new ExamPaperSnapshotDTO());

        gradePaperService.autoGradeObjectiveQuestions(EXAM_ID, STUDENT_ID);

        verify(studentAnswerMapper, never()).batchUpdateScore(anyList());
    }

    @Test
    @DisplayName("自动批阅 混合题型 -> 仅单选对与多选错被评分")
    void autoGrade_mixedTypes() {
        when(gradePaperMapper.getExamAnswerDetail(EXAM_ID, STUDENT_ID)).thenReturn(List.of(
                answerOf(1, 100, "single", "A", 2.0),
                answerOf(2, 101, "multiple", "AC", 4.0),
                answerOf(3, 102, "essay", "要点", 10.0),
                answerOf(4, 103, "judge", "A", 1.0)));
        when(examPaperSnapshotService.getRequired(EXAM_ID))
                .thenReturn(snapshotOf(questionOf(100, "A"), questionOf(101, "AB")));

        gradePaperService.autoGradeObjectiveQuestions(EXAM_ID, STUDENT_ID);

        List<BatchGradeDTO> graded = captureGraded();
        assertEquals(2, graded.size());
        BatchGradeDTO single = graded.stream().filter(d -> d.getId().equals(1)).findFirst().orElseThrow();
        assertEquals(1, single.getIsCorrect().intValue());
        assertEquals(2.0, single.getScore(), 1e-9);
        BatchGradeDTO multiple = graded.stream().filter(d -> d.getId().equals(2)).findFirst().orElseThrow();
        assertEquals(0, multiple.getIsCorrect().intValue());
        assertEquals(0.0, multiple.getScore(), 1e-9);
    }

    // ===================== getGradeStudentList =====================

    @Test
    @DisplayName("getGradeStudentList 入参为null -> 抛 PARAM_ERROR")
    void getGradeStudentList_nullParam() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> gradePaperService.getGradeStudentList(null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verify(examStudentService, never()).list(any(Wrapper.class));
    }

    @Test
    @DisplayName("getGradeStudentList 无关联学生 -> 返回空列表且不查用户")
    void getGradeStudentList_noStudents() {
        when(examStudentService.list(any(Wrapper.class))).thenReturn(List.of());

        List<GradeStudentVO> result = gradePaperService.getGradeStudentList(EXAM_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userFeignClient, never()).listUserBriefs(any());
    }

    @Test
    @DisplayName("getGradeStudentList 正常 -> 映射姓名/学号/分项得分/批阅状态")
    void getGradeStudentList_normal() {
        when(examStudentService.list(any(Wrapper.class))).thenReturn(List.of(
                examStudentOf(20, StudentExamEnum.SUBMITTED),
                examStudentOf(21, StudentExamEnum.GRADED)));
        when(studentAnswerMapper.selectScoreSummary(eq(EXAM_ID), anyList())).thenReturn(List.of(
                summaryOf(20, 8.0, 5.0, 13.0),
                summaryOf(21, 6.0, 4.0, 10.0)));
        when(userFeignClient.listUserBriefs(any())).thenReturn(List.of(
                userOf(20, "张三", "1001"),
                userOf(21, "李四", "1002")));

        List<GradeStudentVO> result = gradePaperService.getGradeStudentList(EXAM_ID);

        assertEquals(2, result.size());
        GradeStudentVO v1 = result.stream().filter(v -> v.getStudentId().equals(20)).findFirst().orElseThrow();
        assertEquals("张三", v1.getStudentName());
        assertEquals("1001", v1.getStudentNo());
        assertEquals(8.0, v1.getObjectiveScore(), 1e-9);
        assertEquals(5.0, v1.getSubjectiveScore(), 1e-9);
        assertEquals(13.0, v1.getTotalScore(), 1e-9);
        assertEquals("待批阅", v1.getStatus());
        GradeStudentVO v2 = result.stream().filter(v -> v.getStudentId().equals(21)).findFirst().orElseThrow();
        assertEquals("李四", v2.getStudentName());
        assertEquals(10.0, v2.getTotalScore(), 1e-9);
        assertEquals("已批阅", v2.getStatus());
    }

    @Test
    @DisplayName("getGradeStudentList 无成绩汇总 -> 三项得分均为0")
    void getGradeStudentList_noSummary_zeros() {
        when(examStudentService.list(any(Wrapper.class)))
                .thenReturn(List.of(examStudentOf(20, StudentExamEnum.SUBMITTED)));
        when(studentAnswerMapper.selectScoreSummary(eq(EXAM_ID), anyList())).thenReturn(List.of());
        when(userFeignClient.listUserBriefs(any()))
                .thenReturn(List.of(userOf(20, "张三", "1001")));

        List<GradeStudentVO> result = gradePaperService.getGradeStudentList(EXAM_ID);

        assertEquals(1, result.size());
        GradeStudentVO vo = result.get(0);
        assertEquals(0.0, vo.getObjectiveScore(), 1e-9);
        assertEquals(0.0, vo.getSubjectiveScore(), 1e-9);
        assertEquals(0.0, vo.getTotalScore(), 1e-9);
        assertEquals("待批阅", vo.getStatus());
    }

    @Test
    @DisplayName("getGradeStudentList 学生存在但查不到用户 -> 返回空列表")
    void getGradeStudentList_usersEmpty() {
        when(examStudentService.list(any(Wrapper.class)))
                .thenReturn(List.of(examStudentOf(20, StudentExamEnum.SUBMITTED)));
        when(studentAnswerMapper.selectScoreSummary(eq(EXAM_ID), anyList())).thenReturn(List.of());
        when(userFeignClient.listUserBriefs(any())).thenReturn(List.of());

        List<GradeStudentVO> result = gradePaperService.getGradeStudentList(EXAM_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
