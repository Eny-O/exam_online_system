package com.ai.exam_spring.modules.grade.service.impl;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.BatchGradeDTO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.ExamAnswerVO;
import com.ai.exam_spring.modules.exam.service.IExamManagementService;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.grade.mapper.GradePaperMapper;
import com.ai.exam_spring.modules.grade.mapper.StudentAnswerMapper;
import com.ai.exam_spring.modules.grade.pojo.vo.GradePaperContentVO;
import com.ai.exam_spring.modules.grade.pojo.vo.GradePaperQuestionVO;
import com.ai.exam_spring.modules.grade.pojo.vo.GradeStudentVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperContentVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperQuestionVO;
import com.ai.exam_spring.modules.paper.service.IPaperManagementService;
import com.ai.exam_spring.modules.question.pojo.enums.OptionLabelEnum;
import com.ai.exam_spring.modules.question.pojo.enums.QuestionTypeEnum;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionCompleteVO;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionOptionVO;
import com.ai.exam_spring.modules.user.pojo.dos.User;
import com.ai.exam_spring.modules.user.service.user.UserService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.session.SqlSessionFactory;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GradePaperServiceImplTest {

    @Mock
    private GradePaperMapper gradePaperMapper;
    @Mock
    private StudentAnswerMapper studentAnswerMapper;
    @Mock
    private IPaperManagementService paperManagementService;
    @Mock
    private IExamManagementService examManagementService;
    @Mock
    private IExamStudentService examStudentService;
    @Mock
    private UserService userService;
    @Mock
    private SqlSessionFactory sqlSessionFactory;

    @InjectMocks
    private GradePaperServiceImpl gradePaperService;

    private static final Integer EXAM_ID = 10;
    private static final Integer PAPER_ID = 5;

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

    // ===================== autoGradeObjectiveQuestions =====================

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

    // ===================== getGradePaper =====================

    @Test
    @DisplayName("getGradePaper 考试不存在 -> 抛 EXAM_NOT_FOUND")
    void getGradePaper_examNotFound() {
        when(examManagementService.getById(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gradePaperService.getGradePaper(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_NOT_FOUND.getCode(), ex.getErrorCode());
        verify(paperManagementService, never()).getPaperById(anyInt());
    }

    @Test
    @DisplayName("getGradePaper 试卷id为null -> 抛 PAPER_NOT_EXIST")
    void getGradePaper_paperIdNull() {
        Exam exam = new Exam();
        exam.setPaperId(null);
        when(examManagementService.getById(any())).thenReturn(exam);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gradePaperService.getGradePaper(EXAM_ID));
        assertEquals(ErrorCodeEnum.PAPER_NOT_EXIST.getCode(), ex.getErrorCode());
        verify(paperManagementService, never()).getPaperById(anyInt());
    }

    @Test
    @DisplayName("getGradePaper 试卷无题目 -> 返回VO, 题目列表为null不抛异常")
    void getGradePaper_noQuestions() {
        Exam exam = new Exam();
        exam.setPaperId(PAPER_ID);
        PaperContentVO pc = new PaperContentVO();
        pc.setId(PAPER_ID);
        pc.setName("期中试卷");
        pc.setTotalScore(100.0);
        pc.setPaperQuestionVOS(null);

        when(examManagementService.getById(any())).thenReturn(exam);
        when(paperManagementService.getPaperById(anyInt())).thenReturn(pc);

        GradePaperContentVO result = gradePaperService.getGradePaper(EXAM_ID);

        assertNotNull(result);
        assertEquals("期中试卷", result.getName());
        assertEquals(100.0, result.getTotalScore());
        assertNull(result.getGradePaperQuestionVOS());
    }

    @Test
    @DisplayName("getGradePaper 正常有题目 -> 题目列表与字段正确映射")
    void getGradePaper_withQuestions() {
        Exam exam = new Exam();
        exam.setPaperId(PAPER_ID);

        QuestionOptionVO opt = new QuestionOptionVO();
        opt.setId(1);
        opt.setLabel(OptionLabelEnum.A);
        opt.setContent("选项A");

        QuestionCompleteVO qc = new QuestionCompleteVO();
        qc.setId(100);
        qc.setContent("1+1=?");
        qc.setAnswer("A");
        qc.setType(QuestionTypeEnum.SINGLE);
        qc.setOptions(List.of(opt));

        PaperQuestionVO pq = new PaperQuestionVO();
        pq.setSortOrder(1);
        pq.setScore(2.0);
        pq.setQuestionCompleteVO(qc);

        PaperContentVO pc = new PaperContentVO();
        pc.setId(PAPER_ID);
        pc.setName("期中试卷");
        pc.setTotalScore(100.0);
        pc.setPaperQuestionVOS(List.of(pq));

        when(examManagementService.getById(any())).thenReturn(exam);
        when(paperManagementService.getPaperById(anyInt())).thenReturn(pc);

        GradePaperContentVO result = gradePaperService.getGradePaper(EXAM_ID);

        assertNotNull(result);
        List<GradePaperQuestionVO> questions = result.getGradePaperQuestionVOS();
        assertNotNull(questions);
        assertEquals(1, questions.size());
        GradePaperQuestionVO gq = questions.get(0);
        assertEquals(1, gq.getSortOrder());
        assertEquals(2.0, gq.getScore());
        assertEquals(100, gq.getId());
        assertEquals("1+1=?", gq.getContent());
        assertEquals("A", gq.getCorrectAnswer());
        assertEquals("SINGLE", gq.getType());
        assertNotNull(gq.getOptions());
        assertEquals(1, gq.getOptions().size());
    }

    @Test
    @DisplayName("getGradePaper 题目questionCompleteVO为null -> 抛 PAPER_INCOMPLETE_NOT_GRADE(试卷不完整, 不能阅卷)")
    void getGradePaper_questionCompleteNull_throws() {
        Exam exam = new Exam();
        exam.setPaperId(PAPER_ID);

        PaperQuestionVO pq = new PaperQuestionVO();
        pq.setSortOrder(1);
        pq.setScore(2.0);
        pq.setQuestionCompleteVO(null); // 数据异常: 题目下挂的题完整信息为null

        PaperContentVO pc = new PaperContentVO();
        pc.setId(PAPER_ID);
        pc.setName("期中试卷");
        pc.setTotalScore(100.0);
        pc.setPaperQuestionVOS(List.of(pq));

        when(examManagementService.getById(any())).thenReturn(exam);
        when(paperManagementService.getPaperById(anyInt())).thenReturn(pc);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gradePaperService.getGradePaper(EXAM_ID));
        assertEquals(ErrorCodeEnum.PAPER_INCOMPLETE_NOT_GRADE.getCode(), ex.getErrorCode());
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
        verify(userService, never()).listByIds(any());
    }

    @Test
    @DisplayName("getGradeStudentList 正常多个学生 -> 正确映射姓名/学号")
    void getGradeStudentList_normal() {
        ExamStudent es1 = new ExamStudent();
        es1.setExamId(EXAM_ID);
        es1.setStudentId(20);
        es1.setStatus(StudentExamEnum.SUBMITTED);
        ExamStudent es2 = new ExamStudent();
        es2.setExamId(EXAM_ID);
        es2.setStudentId(21);
        es2.setStatus(StudentExamEnum.GRADED);
        when(examStudentService.list(any(Wrapper.class))).thenReturn(List.of(es1, es2));

        User u1 = new User();
        u1.setId(20);
        u1.setRealName("张三");
        u1.setStudentNo("1001");
        User u2 = new User();
        u2.setId(21);
        u2.setRealName("李四");
        u2.setStudentNo("1002");
        when(userService.listByIds(any())).thenReturn(List.of(u1, u2));

        List<GradeStudentVO> result = gradePaperService.getGradeStudentList(EXAM_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
        GradeStudentVO v1 = result.stream().filter(v -> v.getStudentId().equals(20)).findFirst().orElseThrow();
        assertEquals("张三", v1.getStudentName());
        assertEquals("1001", v1.getStudentNo());
        GradeStudentVO v2 = result.stream().filter(v -> v.getStudentId().equals(21)).findFirst().orElseThrow();
        assertEquals("李四", v2.getStudentName());
        assertEquals("1002", v2.getStudentNo());
    }

    @Test
    @DisplayName("getGradeStudentList 学生存在但查不到用户 -> 返回空列表")
    void getGradeStudentList_usersEmpty() {
        ExamStudent es = new ExamStudent();
        es.setExamId(EXAM_ID);
        es.setStudentId(20);
        es.setStatus(StudentExamEnum.SUBMITTED);
        when(examStudentService.list(any(Wrapper.class))).thenReturn(List.of(es));
        when(userService.listByIds(any())).thenReturn(List.of());

        List<GradeStudentVO> result = gradePaperService.getGradeStudentList(EXAM_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
