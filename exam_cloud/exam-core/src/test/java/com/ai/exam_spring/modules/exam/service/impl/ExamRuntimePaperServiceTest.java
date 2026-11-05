package com.ai.exam_spring.modules.exam.service.impl;

import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_spring.modules.exam.mapper.ExamManagementMapper;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamPaperSnapshotDTO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamPaperVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamVO;
import com.ai.exam_spring.modules.exam.service.IExamPaperSnapshotService;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperVO;
import com.ai.exam_spring.utils.RedisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamRuntimePaperServiceTest {

    @Mock private RedisUtil redisUtil;
    @Mock private IExamPaperSnapshotService examPaperSnapshotService;
    @Mock private ExamManagementMapper examManagementMapper;

    @InjectMocks private ExamRuntimePaperService examRuntimePaperService;

    private static final Integer EXAM_ID = 10;

    private StuExamPaperVO cachedPaper(LocalDateTime startTime, Integer duration) {
        StuExamVO exam = new StuExamVO();
        exam.setStartTime(startTime);
        exam.setDuration(duration);
        StuExamPaperVO vo = new StuExamPaperVO();
        vo.setExam(exam);
        vo.setPaper(new StuPaperVO());
        lenient().when(redisUtil.get(eq(RedisConstant.EXAM_PUBLISH_PREFIX + EXAM_ID),
                eq(StuExamPaperVO.class))).thenReturn(vo);
        return vo;
    }

    private void examAt(LocalDateTime startTime, Integer duration) {
        Exam exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setStartTime(startTime);
        exam.setDuration(duration);
        lenient().when(examManagementMapper.selectById(EXAM_ID)).thenReturn(exam);
    }

    @Test
    @DisplayName("未到开考时间：取卷抛 EXAM_NOT_STARTED")
    void requireWithinExamWindow_notStarted() {
        cachedPaper(LocalDateTime.now().plusMinutes(10), 60);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examRuntimePaperService.requireWithinExamWindow(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_NOT_STARTED.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("考试进行中：放行并返回卷面数据")
    void requireWithinExamWindow_ongoing() {
        StuExamPaperVO vo = cachedPaper(LocalDateTime.now().minusMinutes(10), 60);

        StuExamPaperVO result = examRuntimePaperService.requireWithinExamWindow(EXAM_ID);

        assertSame(vo, result);
    }

    @Test
    @DisplayName("结束超过宽限期：取卷抛 EXAM_ALREADY_ENDED")
    void requireWithinExamWindow_endedBeyondGrace() {
        cachedPaper(LocalDateTime.now().minusMinutes(120), 60);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examRuntimePaperService.requireWithinExamWindow(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_ALREADY_ENDED.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("结束后仍处于宽限期内：放行")
    void requireWithinExamWindow_withinGrace() {
        cachedPaper(LocalDateTime.now().minusMinutes(60).minusSeconds(30), 60);

        assertDoesNotThrow(() -> examRuntimePaperService.requireWithinExamWindow(EXAM_ID));
    }

    @Test
    @DisplayName("交卷只校验已开考：考试结束后仍可提交")
    void assertExamStarted_endedStillAllowed() {
        cachedPaper(LocalDateTime.now().minusMinutes(120), 60);
        examAt(LocalDateTime.now().minusMinutes(120), 60);

        assertDoesNotThrow(() -> examRuntimePaperService.assertExamStarted(EXAM_ID));
        assertThrows(BusinessException.class,
                () -> examRuntimePaperService.requireWithinExamWindow(EXAM_ID));
    }

    @Test
    @DisplayName("交卷校验未开考：抛 EXAM_NOT_STARTED")
    void assertExamStarted_notStarted() {
        examAt(LocalDateTime.now().plusMinutes(10), 60);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examRuntimePaperService.assertExamStarted(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_NOT_STARTED.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("缺少开考时间或时长：跳过时间窗校验")
    void requireWithinExamWindow_missingTime() {
        cachedPaper(null, 60);
        examAt(null, 60);

        assertDoesNotThrow(() -> examRuntimePaperService.requireWithinExamWindow(EXAM_ID));
        assertDoesNotThrow(() -> examRuntimePaperService.assertExamStarted(EXAM_ID));
        assertDoesNotThrow(() -> examRuntimePaperService.assertWithinExamWindow(EXAM_ID));
    }

    @Test
    @DisplayName("仅做时间窗校验时：只读考试表，不读卷面缓存与快照")
    void assertWithinExamWindow_readsExamTableOnly() {
        examAt(LocalDateTime.now().minusMinutes(10), 60);

        assertDoesNotThrow(() -> examRuntimePaperService.assertWithinExamWindow(EXAM_ID));
        assertDoesNotThrow(() -> examRuntimePaperService.assertExamStarted(EXAM_ID));

        verify(redisUtil, never()).get(eq(RedisConstant.EXAM_PUBLISH_PREFIX + EXAM_ID),
                eq(StuExamPaperVO.class));
        verify(examPaperSnapshotService, never()).getRequired(any());
    }

    @Test
    @DisplayName("未到开考时间：仅时间窗校验同样抛 EXAM_NOT_STARTED")
    void assertWithinExamWindow_notStarted() {
        examAt(LocalDateTime.now().plusMinutes(10), 60);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examRuntimePaperService.assertWithinExamWindow(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_NOT_STARTED.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("考试不存在：时间窗校验抛 EXAM_NOT_FOUND")
    void assertWithinExamWindow_examNotFound() {
        lenient().when(examManagementMapper.selectById(EXAM_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examRuntimePaperService.assertWithinExamWindow(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_NOT_FOUND.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("缓存异常时回源快照")
    void getRuntimePaper_fallbackToSnapshot() {
        when(redisUtil.get(eq(RedisConstant.EXAM_PUBLISH_PREFIX + EXAM_ID),
                eq(StuExamPaperVO.class))).thenThrow(new RuntimeException("redis down"));
        StuExamPaperVO snapshotPaper = new StuExamPaperVO();
        snapshotPaper.setExam(new StuExamVO());
        snapshotPaper.setPaper(new StuPaperVO());
        when(examPaperSnapshotService.getRequired(EXAM_ID)).thenReturn(new ExamPaperSnapshotDTO());
        when(examPaperSnapshotService.toStuExamPaper(any(ExamPaperSnapshotDTO.class)))
                .thenReturn(snapshotPaper);

        StuExamPaperVO result = examRuntimePaperService.getRuntimePaper(EXAM_ID);

        assertSame(snapshotPaper, result);
    }
}
