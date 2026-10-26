package com.ai.exam_spring.modules.exam.service.impl;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.modules.grade.pojo.dto.GradeMessageDTO;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.mapper.StuExamMapper;
import com.ai.exam_spring.modules.exam.service.IExamManagementService;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.grade.service.IStudentAnswerService;
import com.ai.exam_spring.modules.grade.mapper.StudentAnswerMapper;
import com.ai.exam_spring.utils.RedisUtil;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StuExamServiceImplTest {

    @Mock private IExamManagementService examManagementService;
    @Mock private IStudentAnswerService studentAnswerService;
    @Mock private IExamStudentService examStudentService;
    @Mock private StuExamMapper stuExamMapper;
    @Mock private StudentAnswerMapper studentAnswerMapper;
    @Mock private RocketMQTemplate rocketMQTemplate;
    @Mock private RedisUtil redisUtil;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks private StuExamServiceImpl stuExamService;

    private static final Integer EXAM_ID = 10;
    private static final Integer STUDENT_ID = 20;

    @BeforeEach
    void setUp() {
        when(securityUtil.getCurrentUserId()).thenReturn(STUDENT_ID.toString());
        // 让 TransactionTemplate 真正执行回调（模拟事务提交）
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    @Test
    @DisplayName("submitPaper 正常提交：状态流转SUBMITTED + 批量插入答案 + 发MQ")
    void submitPaper_normal() {
        Map<String, String> raw = Map.of("1", "{\"studentAnswer\":\"A\",\"fullScore\":2.0}");
        when(redisUtil.hGetAllRaw(anyString())).thenReturn(raw);

        BaseMapper<ExamStudent> baseMapper = mock(BaseMapper.class);
        when(examStudentService.getBaseMapper()).thenReturn(baseMapper);
        when(baseMapper.update(any(), any())).thenReturn(1);

        stuExamService.submitPaper(EXAM_ID);

        verify(studentAnswerMapper).batchInsert(anyList());
        verify(rocketMQTemplate).convertAndSend(eq("exam-grade-topic"), any(GradeMessageDTO.class));
    }

    @Test
    @DisplayName("submitPaper 重复提交：update影响0行抛异常，不写答案")
    void submitPaper_duplicate() {
        Map<String, String> raw = Map.of("1", "{\"studentAnswer\":\"A\",\"fullScore\":2.0}");
        when(redisUtil.hGetAllRaw(anyString())).thenReturn(raw);

        BaseMapper<ExamStudent> baseMapper = mock(BaseMapper.class);
        when(examStudentService.getBaseMapper()).thenReturn(baseMapper);
        when(baseMapper.update(any(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> stuExamService.submitPaper(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_STATUS_NOT_PENDING_NOT_SUBMIT.getCode(), ex.getErrorCode());
        verify(studentAnswerMapper, never()).batchInsert(anyList());
    }

    @Test
    @DisplayName("submitPaper 空答案：抛 NOT_SUBMIT_EMPTY_PAPER，不进事务")
    void submitPaper_empty() {
        when(redisUtil.hGetAllRaw(anyString())).thenReturn(Map.of());

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
}
