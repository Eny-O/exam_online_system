package com.ai.exam_spring.modules.exam.service.impl;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.security.SecurityUtil;
import com.ai.exam_spring.modules.grade.pojo.dto.GradeMessageDTO;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.dos.stu_answer.StudentAnswer;
import com.ai.exam_spring.modules.exam.pojo.enums.ExamEnum;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.mapper.StuExamMapper;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamResultVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.BaseStudentAnswerVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StudentAnswerVO;
import com.ai.exam_spring.modules.exam.service.IExamManagementService;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.grade.service.IStudentAnswerService;
import com.ai.exam_spring.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StuExamServiceImplTest {

    @Mock private IExamManagementService examManagementService;
    @Mock private IStudentAnswerService studentAnswerService;
    @Mock private IExamStudentService examStudentService;
    @Mock private StuExamMapper stuExamMapper;
    @Mock private ExamAnswerPersistenceService examAnswerPersistenceService;
    @Mock private RocketMQTemplate rocketMQTemplate;
    @Mock private RedisUtil redisUtil;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks private StuExamServiceImpl stuExamService;

    private static final Integer EXAM_ID = 10;
    private static final Integer STUDENT_ID = 20;

    @BeforeEach
    void setUp() {
        // lenient: examInfoById 不调用 getCurrentUserId，避免被严格 stub 判定为多余
        lenient().when(securityUtil.getCurrentUserId()).thenReturn(STUDENT_ID.toString());
        // 让 TransactionTemplate 真正执行回调（模拟事务提交）
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    @Test
    @DisplayName("submitPaper 正常提交：状态流转SUBMITTED + 落库答案 + 发MQ")
    void submitPaper_normal() {
        List<StudentAnswer> answers = List.of(new StudentAnswer());
        when(examAnswerPersistenceService.loadAnswersFromRedis(EXAM_ID, STUDENT_ID)).thenReturn(answers);

        BaseMapper<ExamStudent> baseMapper = mock(BaseMapper.class);
        when(examStudentService.getBaseMapper()).thenReturn(baseMapper);
        when(baseMapper.update(any(), any())).thenReturn(1);

        stuExamService.submitPaper(EXAM_ID);

        verify(examAnswerPersistenceService).saveAnswers(answers);
        verify(rocketMQTemplate).convertAndSend(eq("exam-grade-topic"), any(GradeMessageDTO.class));
    }

    @Test
    @DisplayName("submitPaper 重复提交：update影响0行抛异常，不写答案")
    void submitPaper_duplicate() {
        List<StudentAnswer> answers = List.of(new StudentAnswer());
        when(examAnswerPersistenceService.loadAnswersFromRedis(EXAM_ID, STUDENT_ID)).thenReturn(answers);

        BaseMapper<ExamStudent> baseMapper = mock(BaseMapper.class);
        when(examStudentService.getBaseMapper()).thenReturn(baseMapper);
        when(baseMapper.update(any(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> stuExamService.submitPaper(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_STATUS_NOT_PENDING_NOT_SUBMIT.getCode(), ex.getErrorCode());
        verify(examAnswerPersistenceService, never()).saveAnswers(anyList());
    }

    @Test
    @DisplayName("submitPaper 空答案：抛 NOT_SUBMIT_EMPTY_PAPER，不进事务")
    void submitPaper_empty() {
        when(examAnswerPersistenceService.loadAnswersFromRedis(EXAM_ID, STUDENT_ID)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> stuExamService.submitPaper(EXAM_ID));
        assertEquals(ErrorCodeEnum.NOT_SUBMIT_EMPTY_PAPER.getCode(), ex.getErrorCode());
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    @DisplayName("initStuExam 考试不存在：抛 EXAM_NOT_FOUND")
    void initStuExam_examNotFound() {
        when(examManagementService.getById(EXAM_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> stuExamService.initStuExam(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_NOT_FOUND.getCode(), ex.getErrorCode());
        verify(examStudentService, never()).getOne(any());
    }

    @Test
    @DisplayName("getExamResult 状态非GRADED：抛 EXAM_STATUS_NOT_GRADED")
    void getExamResult_notGraded() {
        ExamStudent es = new ExamStudent();
        es.setStatus(StudentExamEnum.SUBMITTED);
        when(examStudentService.getOne(any(LambdaQueryWrapper.class))).thenReturn(es);

        BusinessException ex = assertThrows(BusinessException.class, () -> stuExamService.getExamResult(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_STATUS_NOT_GRADED.getCode(), ex.getErrorCode());
    }

    // ===================== examInfoById =====================

    @Test
    @DisplayName("examInfoById 考试不存在 -> 抛 EXAM_NOT_FOUND")
    void examInfoById_examNotFound() {
        when(examManagementService.getById(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stuExamService.examInfoById(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_NOT_FOUND.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("examInfoById 正常 -> 拷贝为 StuExamVO 且字段正确")
    void examInfoById_normal() {
        Exam exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setExamName("期末数学");
        exam.setStatus(ExamEnum.ONGOING);
        exam.setPaperId(5);
        when(examManagementService.getById(any())).thenReturn(exam);

        StuExamVO result = stuExamService.examInfoById(EXAM_ID);

        assertNotNull(result);
        assertEquals(EXAM_ID, result.getId());
        assertEquals("期末数学", result.getExamName());
        assertEquals(ExamEnum.ONGOING, result.getStatus());
        assertEquals(5, result.getPaperId());
    }

    // ===================== getAnswerDetail =====================

    @Test
    @DisplayName("getAnswerDetail 无答案 -> 返回空列表")
    void getAnswerDetail_empty() {
        when(studentAnswerService.getStudentAnswer(eq(STUDENT_ID), eq(EXAM_ID))).thenReturn(List.of());

        List<BaseStudentAnswerVO> result = stuExamService.getAnswerDetail(EXAM_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(studentAnswerService).getStudentAnswer(STUDENT_ID, EXAM_ID);
    }

    @Test
    @DisplayName("getAnswerDetail 正常 -> 映射为 BaseStudentAnswerVO 且字段透传")
    void getAnswerDetail_normal() {
        StudentAnswerVO a1 = new StudentAnswerVO();
        a1.setId(1);
        a1.setQuestionId(101);
        a1.setStudentAnswer("A");
        a1.setIsCorrect(1);
        a1.setScore(2.0);
        StudentAnswerVO a2 = new StudentAnswerVO();
        a2.setId(2);
        a2.setQuestionId(102);
        a2.setStudentAnswer("B");
        a2.setIsCorrect(0);
        a2.setScore(0.0);
        when(studentAnswerService.getStudentAnswer(eq(STUDENT_ID), eq(EXAM_ID))).thenReturn(List.of(a1, a2));

        List<BaseStudentAnswerVO> result = stuExamService.getAnswerDetail(EXAM_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
        BaseStudentAnswerVO r1 = result.get(0);
        assertEquals(1, r1.getId());
        assertEquals(101, r1.getQuestionId());
        assertEquals("A", r1.getStudentAnswer());
        assertEquals(1, r1.getIsCorrect());
        assertEquals(2.0, r1.getScore());
        BaseStudentAnswerVO r2 = result.get(1);
        assertEquals(2, r2.getId());
        assertEquals(102, r2.getQuestionId());
        assertEquals("B", r2.getStudentAnswer());
        assertEquals(0, r2.getIsCorrect());
        assertEquals(0.0, r2.getScore());
    }

    // ===================== getAllExamResult =====================

    @Test
    @DisplayName("getAllExamResult 无结果 -> 返回空列表")
    void getAllExamResult_empty() {
        when(stuExamMapper.getAllExamResult(eq(STUDENT_ID))).thenReturn(List.of());

        List<ExamResultVO> result = stuExamService.getAllExamResult();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(stuExamMapper).getAllExamResult(STUDENT_ID);
    }

    @Test
    @DisplayName("getAllExamResult 正常 -> 透传 ExamResultVO 列表且按当前用户查询")
    void getAllExamResult_normal() {
        ExamResultVO r1 = new ExamResultVO();
        r1.setExamId(10);
        r1.setExamName("期末数学");
        r1.setExamScore(88.0);
        ExamResultVO r2 = new ExamResultVO();
        r2.setExamId(11);
        r2.setExamName("期中英语");
        r2.setExamScore(92.0);
        when(stuExamMapper.getAllExamResult(eq(STUDENT_ID))).thenReturn(List.of(r1, r2));

        List<ExamResultVO> result = stuExamService.getAllExamResult();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10, result.get(0).getExamId());
        assertEquals("期末数学", result.get(0).getExamName());
        assertEquals(88.0, result.get(0).getExamScore());
        assertEquals(11, result.get(1).getExamId());
        assertEquals("期中英语", result.get(1).getExamName());
        verify(stuExamMapper).getAllExamResult(STUDENT_ID);
    }
}
