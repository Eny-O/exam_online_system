package com.ai.exam_spring.modules.exam.service.impl;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamPaperSnapshotDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamStatusUpdateDTO;
import com.ai.exam_spring.modules.exam.pojo.enums.ExamEnum;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.mapper.ExamManagementMapper;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamPaperVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamVO;
import com.ai.exam_spring.modules.exam.service.IExamPaperSnapshotService;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperContentVO;
import com.ai.exam_spring.modules.paper.service.IPaperManagementService;
import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_spring.utils.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamManagementServiceImplTest {

    @Mock
    private ExamManagementMapper examManagementMapper;
    @Mock
    private IExamStudentService examStudentService;
    @Mock
    private IPaperManagementService paperManagementService;
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private IExamPaperSnapshotService examPaperSnapshotService;
    @Mock
    private ExamRuntimePaperService examRuntimePaperService;

    @InjectMocks
    private ExamManagementServiceImpl examManagementService;

    private static final Integer EXAM_ID = 10;
    private static final Integer PAPER_ID = 100;

    private Exam examOf(ExamEnum status) {
        Exam e = new Exam();
        e.setId(EXAM_ID);
        e.setPaperId(PAPER_ID);
        e.setStatus(status);
        e.setVersion(1);
        return e;
    }

    @BeforeEach
    void setUp() {
        // updateById 默认成功；非法流转用例根本不调用，故用 lenient 避免 strict stubbing 报错
        lenient().when(examManagementMapper.updateById(any(Exam.class))).thenReturn(1);
        // 全员已批阅（默认）；批阅校验用例会覆盖为"含未批阅学生"
        lenient().when(examStudentService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
    }

    // ========== updateExamStatus：防御 ==========

    @Test
    @DisplayName("updateExamStatus 入参null -> PARAM_ERROR")
    void update_null() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.updateExamStatus(null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
    }

    @Test
    @DisplayName("updateExamStatus 考试不存在 -> EXAM_NOT_FOUND")
    void update_notExist() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.ALREADY_POST)));
        assertEquals(ErrorCodeEnum.EXAM_NOT_FOUND.getCode(), ex.getErrorCode());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
    }

    // ========== updateExamStatus：合法流转 ==========

    @Test
    @DisplayName("NOT_POST -> ALREADY_POST 合法")
    void transfer_notPost_to_alreadyPost() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_POST));

        examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.ALREADY_POST));

        verify(examManagementMapper).updateById(any(Exam.class));
    }

    @Test
    @DisplayName("ALREADY_POST -> NOT_START 合法")
    void transfer_alreadyPost_to_notStart() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.ALREADY_POST));

        examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.NOT_START));

        verify(examManagementMapper).updateById(any(Exam.class));
    }

    @Test
    @DisplayName("ALREADY_POST -> PENDING_REVIEW 合法")
    void transfer_alreadyPost_to_pendingReview() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.ALREADY_POST));

        examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.PENDING_REVIEW));

        verify(examManagementMapper).updateById(any(Exam.class));
    }

    @Test
    @DisplayName("NOT_START -> ALREADY_POST 合法")
    void transfer_notStart_to_alreadyPost() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_START));

        examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.ALREADY_POST));

        verify(examManagementMapper).updateById(any(Exam.class));
    }

    @Test
    @DisplayName("NOT_START -> PENDING_REVIEW 合法")
    void transfer_notStart_to_pendingReview() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_START));

        examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.PENDING_REVIEW));

        verify(examManagementMapper).updateById(any(Exam.class));
    }

    @Test
    @DisplayName("ONGOING -> PENDING_REVIEW 合法")
    void transfer_ongoing_to_pendingReview() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.ONGOING));

        examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.PENDING_REVIEW));

        verify(examManagementMapper).updateById(any(Exam.class));
    }

    @Test
    @DisplayName("PENDING_REVIEW -> REVIEW_FINISHED 合法（全员已批阅）")
    void transfer_pendingReview_to_reviewFinished() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.PENDING_REVIEW));

        examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.REVIEW_FINISHED));

        verify(examManagementMapper).updateById(any(Exam.class));
    }

    // ========== updateExamStatus：非法流转 ==========

    @Test
    @DisplayName("NOT_POST -> NOT_START 非法 -> EXAM_STATUS_CHANGE_FAIL")
    void transfer_illegal_notPost_to_notStart() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_POST));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.NOT_START)));
        assertEquals(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL.getCode(), ex.getErrorCode());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
    }

    @Test
    @DisplayName("NOT_POST -> PENDING_REVIEW 非法 -> EXAM_STATUS_CHANGE_FAIL")
    void transfer_illegal_notPost_to_pendingReview() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_POST));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.PENDING_REVIEW)));
        assertEquals(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL.getCode(), ex.getErrorCode());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
    }

    @Test
    @DisplayName("REVIEW_FINISHED(终态) -> PENDING_REVIEW 非法 -> EXAM_STATUS_CHANGE_FAIL")
    void transfer_illegal_terminal() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.REVIEW_FINISHED));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.PENDING_REVIEW)));
        assertEquals(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL.getCode(), ex.getErrorCode());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
    }

    @Test
    @DisplayName("自环 NOT_POST -> NOT_POST 非法 -> EXAM_STATUS_CHANGE_FAIL")
    void transfer_illegal_selfLoop() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_POST));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.NOT_POST)));
        assertEquals(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL.getCode(), ex.getErrorCode());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
    }

    @Test
    @DisplayName("ONGOING -> ALREADY_POST 非法 -> EXAM_STATUS_CHANGE_FAIL")
    void transfer_illegal_ongoing_to_alreadyPost() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.ONGOING));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.ALREADY_POST)));
        assertEquals(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL.getCode(), ex.getErrorCode());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
    }

    // ========== updateExamStatus：批阅校验 ==========

    @Test
    @DisplayName("PENDING_REVIEW -> REVIEW_FINISHED 但存在未批阅学生 -> EXAM_NOT_ALL_GRADED")
    void transfer_reviewFinished_notAllGraded() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.PENDING_REVIEW));
        ExamStudent unGraded = new ExamStudent();
        unGraded.setStatus(StudentExamEnum.SUBMITTED); // 既非 GRADED 也非 ABSENT
        when(examStudentService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(unGraded));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.REVIEW_FINISHED)));
        assertEquals(ErrorCodeEnum.EXAM_NOT_ALL_GRADED.getCode(), ex.getErrorCode());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
    }

    // ========== updateExamStatus：乐观锁 ==========

    @Test
    @DisplayName("合法流转但 updateById 返回0（版本冲突）-> EXAM_STATUS_CHANGE_FAIL")
    void transfer_optimisticLock() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_POST));
        when(examManagementMapper.updateById(any(Exam.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.updateExamStatus(new ExamStatusUpdateDTO(EXAM_ID, ExamEnum.ALREADY_POST)));
        assertEquals(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL.getCode(), ex.getErrorCode());
    }

    // ========== publishExam ==========

    @Test
    @DisplayName("publishExam 正常：生成快照、置为已发布并写入卷面缓存")
    void publish_normal() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_POST));
        when(paperManagementService.getPaperById(PAPER_ID)).thenReturn(new PaperContentVO());
        when(examPaperSnapshotService.getRequired(EXAM_ID)).thenReturn(new ExamPaperSnapshotDTO());
        when(examPaperSnapshotService.toStuExamPaper(any(ExamPaperSnapshotDTO.class)))
                .thenReturn(new StuExamPaperVO());

        examManagementService.publishExam(EXAM_ID);

        verify(examPaperSnapshotService).createOnce(any(Exam.class), any(PaperContentVO.class));
        verify(examManagementMapper).updateById(argThat((Exam e) -> e.getStatus() == ExamEnum.ALREADY_POST));
        verify(redisUtil).setIfAbsent(eq(RedisConstant.EXAM_PUBLISH_PREFIX + EXAM_ID),
                any(Object.class), anyLong());
        verify(redisUtil, never()).set(any(), any(Object.class), anyLong());
    }

    @Test
    @DisplayName("publishExam 已发布且快照存在 -> 幂等返回并恢复卷面缓存")
    void publish_alreadyPublished_idempotent() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.ALREADY_POST));
        when(examPaperSnapshotService.exists(EXAM_ID)).thenReturn(true);

        examManagementService.publishExam(EXAM_ID);

        verify(examRuntimePaperService).restoreCache(EXAM_ID);
        verify(paperManagementService, never()).getPaperById(any());
        verify(examPaperSnapshotService, never()).createOnce(any(), any());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
        verify(redisUtil, never()).setIfAbsent(any(), any(), anyLong());
    }

    @Test
    @DisplayName("publishExam 缓存TTL按快照中的考试时间计算")
    void publish_ttlFromSnapshot() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_POST));
        when(paperManagementService.getPaperById(PAPER_ID)).thenReturn(new PaperContentVO());
        when(examPaperSnapshotService.getRequired(EXAM_ID)).thenReturn(new ExamPaperSnapshotDTO());
        StuExamVO snapshotExam = new StuExamVO();
        snapshotExam.setStartTime(LocalDateTime.now().plusHours(2));
        snapshotExam.setDuration(60);
        StuExamPaperVO examPaperVO = new StuExamPaperVO();
        examPaperVO.setExam(snapshotExam);
        when(examPaperSnapshotService.toStuExamPaper(any(ExamPaperSnapshotDTO.class)))
                .thenReturn(examPaperVO);

        examManagementService.publishExam(EXAM_ID);

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(redisUtil).setIfAbsent(eq(RedisConstant.EXAM_PUBLISH_PREFIX + EXAM_ID),
                any(Object.class), ttlCaptor.capture());
        assertTrue(ttlCaptor.getValue() > 24 * 60 * 60L);
    }

    @Test
    @DisplayName("publishExam 非未发布状态且快照缺失 -> EXAM_PUBLISH_NOT_ALLOWED")
    void publish_alreadyPublished() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.ALREADY_POST));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.publishExam(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_PUBLISH_NOT_ALLOWED.getCode(), ex.getErrorCode());
        verify(examPaperSnapshotService, never()).createOnce(any(), any());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
        verify(redisUtil, never()).setIfAbsent(any(), any(), anyLong());
    }

    @Test
    @DisplayName("publishExam 快照已存在但未发布 -> 不重建快照，补齐状态与缓存")
    void publish_snapshotExists() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_POST));
        when(examPaperSnapshotService.exists(EXAM_ID)).thenReturn(true);
        when(examPaperSnapshotService.getRequired(EXAM_ID)).thenReturn(new ExamPaperSnapshotDTO());
        when(examPaperSnapshotService.toStuExamPaper(any(ExamPaperSnapshotDTO.class)))
                .thenReturn(new StuExamPaperVO());

        examManagementService.publishExam(EXAM_ID);

        verify(paperManagementService, never()).getPaperById(any());
        verify(examPaperSnapshotService, never()).createOnce(any(), any());
        verify(examManagementMapper).updateById(argThat((Exam e) -> e.getStatus() == ExamEnum.ALREADY_POST));
        verify(redisUtil).setIfAbsent(eq(RedisConstant.EXAM_PUBLISH_PREFIX + EXAM_ID),
                any(Object.class), anyLong());
    }

    @Test
    @DisplayName("publishExam 乐观锁冲突 -> EXAM_STATUS_CHANGE_FAIL 且不写缓存")
    void publish_versionConflict() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_POST));
        when(paperManagementService.getPaperById(PAPER_ID)).thenReturn(new PaperContentVO());
        when(examManagementMapper.updateById(any(Exam.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.publishExam(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL.getCode(), ex.getErrorCode());
        verify(redisUtil, never()).setIfAbsent(any(), any(), anyLong());
    }

    @Test
    @DisplayName("publishExam 考试不存在 -> EXAM_NOT_FOUND")
    void publish_examNotFound() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.publishExam(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_NOT_FOUND.getCode(), ex.getErrorCode());
        verify(paperManagementService, never()).getPaperById(any());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
        verify(redisUtil, never()).setIfAbsent(any(), any(), anyLong());
    }

    @Test
    @DisplayName("publishExam 试卷不存在 -> PAPER_NOT_EXIST")
    void publish_paperNotFound() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.NOT_POST));
        when(paperManagementService.getPaperById(PAPER_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.publishExam(EXAM_ID));
        assertEquals(ErrorCodeEnum.PAPER_NOT_EXIST.getCode(), ex.getErrorCode());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
        verify(redisUtil, never()).setIfAbsent(any(), any(), anyLong());
    }

    // ========== clearExamCache ==========

    @Test
    @DisplayName("clearExamCache 正常：删缓存与快照并置回未发布")
    void clearCache_normal() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.ALREADY_POST));

        examManagementService.clearExamCache(EXAM_ID);

        verify(redisUtil).delete(RedisConstant.EXAM_PUBLISH_PREFIX + EXAM_ID);
        verify(redisUtil).deleteByPattern(RedisConstant.EXAM_ANSWER_PREFIX + EXAM_ID + ":*");
        verify(examPaperSnapshotService).deleteSnapshot(EXAM_ID);
        verify(examManagementMapper).updateById(argThat((Exam e) -> e.getStatus() == ExamEnum.NOT_POST));
    }

    @Test
    @DisplayName("clearExamCache 非发布/未发布状态 -> EXAM_STATUS_CHANGE_FAIL")
    void clearCache_statusNotAllowed() {
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(examOf(ExamEnum.ONGOING));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.clearExamCache(EXAM_ID));
        assertEquals(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL.getCode(), ex.getErrorCode());
        verify(examPaperSnapshotService, never()).deleteSnapshot(any());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
    }

}
