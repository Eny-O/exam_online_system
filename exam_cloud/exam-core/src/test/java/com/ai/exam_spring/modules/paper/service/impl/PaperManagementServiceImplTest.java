package com.ai.exam_spring.modules.paper.service.impl;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.status.PaperStatus;
import com.ai.exam_spring.modules.paper.mapper.PaperManagementMapper;
import com.ai.exam_spring.modules.paper.pojo.dos.Paper;
import com.ai.exam_spring.modules.paper.pojo.dto.PaperStatusUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaperManagementServiceImplTest {

    @Mock
    private PaperManagementMapper paperManagementMapper;

    private PaperManagementServiceImpl paperManagementService;

    @BeforeAll
    static void initTableInfo() {
        // 纯单测无 Spring 上下文，MyBatis-Plus 的 LambdaWrapper 解析实体 lambda 需要 TableInfo 缓存，手动初始化一次
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Paper.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        paperManagementService = new PaperManagementServiceImpl();
        // baseMapper 定义在 MyBatis-Plus 父类层级中，向上遍历超类找到并注入 mock
        setField(paperManagementService, "baseMapper", paperManagementMapper);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private Paper paperOf(Integer id, PaperStatus status) {
        Paper p = new Paper();
        p.setId(id);
        p.setStatus(status);
        p.setVersion(1);
        return p;
    }

    private PaperStatusUpdateDTO dtoOf(Integer id, PaperStatus status) {
        PaperStatusUpdateDTO dto = new PaperStatusUpdateDTO();
        dto.setId(id);
        dto.setStatus(status);
        return dto;
    }

    // ===== 防御 =====

    @Test
    @DisplayName("updatePaperStatus 入参null -> PARAM_ERROR")
    void updatePaperStatus_nullDto() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperManagementService.updatePaperStatus(null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verify(paperManagementMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("updatePaperStatus 试卷不存在 -> PAPER_NOT_EXIST")
    void updatePaperStatus_notExist() {
        when(paperManagementMapper.selectById(1)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperManagementService.updatePaperStatus(dtoOf(1, PaperStatus.PENDING_ENABLE)));
        assertEquals(ErrorCodeEnum.PAPER_NOT_EXIST.getCode(), ex.getErrorCode());
        verify(paperManagementMapper, never()).update(any());
    }

    // ===== 合法流转 =====

    @Test
    @DisplayName("DRAFT -> PENDING_ENABLE 合法")
    void transfer_draft_to_pending() {
        when(paperManagementMapper.selectById(1)).thenReturn(paperOf(1, PaperStatus.DRAFT));
        when(paperManagementMapper.update(any())).thenReturn(1);

        assertDoesNotThrow(() -> paperManagementService.updatePaperStatus(dtoOf(1, PaperStatus.PENDING_ENABLE)));
        verify(paperManagementMapper).update(any());
    }

    @Test
    @DisplayName("PENDING_ENABLE -> ALREADY_ENABLE 合法")
    void transfer_pending_to_enable() {
        when(paperManagementMapper.selectById(1)).thenReturn(paperOf(1, PaperStatus.PENDING_ENABLE));
        when(paperManagementMapper.update(any())).thenReturn(1);

        assertDoesNotThrow(() -> paperManagementService.updatePaperStatus(dtoOf(1, PaperStatus.ALREADY_ENABLE)));
        verify(paperManagementMapper).update(any());
    }

    @Test
    @DisplayName("PENDING_ENABLE -> DRAFT 合法(回退)")
    void transfer_pending_to_draft() {
        when(paperManagementMapper.selectById(1)).thenReturn(paperOf(1, PaperStatus.PENDING_ENABLE));
        when(paperManagementMapper.update(any())).thenReturn(1);

        assertDoesNotThrow(() -> paperManagementService.updatePaperStatus(dtoOf(1, PaperStatus.DRAFT)));
        verify(paperManagementMapper).update(any());
    }

    @Test
    @DisplayName("ALREADY_ENABLE -> PENDING_ENABLE 合法(唯一后退)")
    void transfer_enable_to_pending() {
        when(paperManagementMapper.selectById(1)).thenReturn(paperOf(1, PaperStatus.ALREADY_ENABLE));
        when(paperManagementMapper.update(any())).thenReturn(1);

        assertDoesNotThrow(() -> paperManagementService.updatePaperStatus(dtoOf(1, PaperStatus.PENDING_ENABLE)));
        verify(paperManagementMapper).update(any());
    }

    // ===== 非法流转 + 乐观锁 =====

    @Test
    @DisplayName("DRAFT -> ALREADY_ENABLE 非法 -> PAPER_STATUS_TRANSFER_NOT_ALLOWED")
    void transfer_draft_to_enable_illegal() {
        when(paperManagementMapper.selectById(1)).thenReturn(paperOf(1, PaperStatus.DRAFT));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperManagementService.updatePaperStatus(dtoOf(1, PaperStatus.ALREADY_ENABLE)));
        assertEquals(ErrorCodeEnum.PAPER_STATUS_TRANSFER_NOT_ALLOWED.getCode(), ex.getErrorCode());
        verify(paperManagementMapper, never()).update(any());
    }

    @Test
    @DisplayName("ALREADY_ENABLE -> DRAFT 非法 -> PAPER_STATUS_TRANSFER_NOT_ALLOWED")
    void transfer_enable_to_draft_illegal() {
        when(paperManagementMapper.selectById(1)).thenReturn(paperOf(1, PaperStatus.ALREADY_ENABLE));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperManagementService.updatePaperStatus(dtoOf(1, PaperStatus.DRAFT)));
        assertEquals(ErrorCodeEnum.PAPER_STATUS_TRANSFER_NOT_ALLOWED.getCode(), ex.getErrorCode());
        verify(paperManagementMapper, never()).update(any());
    }

    @Test
    @DisplayName("DRAFT -> DRAFT 自环非法 -> PAPER_STATUS_TRANSFER_NOT_ALLOWED")
    void transfer_self_illegal() {
        when(paperManagementMapper.selectById(1)).thenReturn(paperOf(1, PaperStatus.DRAFT));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperManagementService.updatePaperStatus(dtoOf(1, PaperStatus.DRAFT)));
        assertEquals(ErrorCodeEnum.PAPER_STATUS_TRANSFER_NOT_ALLOWED.getCode(), ex.getErrorCode());
        verify(paperManagementMapper, never()).update(any());
    }

    @Test
    @DisplayName("合法流转但update返回0(乐观锁冲突) -> PAPER_STATUS_CHANGE_FAIL")
    void transfer_optimisticLockFail() {
        when(paperManagementMapper.selectById(1)).thenReturn(paperOf(1, PaperStatus.DRAFT));
        when(paperManagementMapper.update(any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperManagementService.updatePaperStatus(dtoOf(1, PaperStatus.PENDING_ENABLE)));
        assertEquals(ErrorCodeEnum.PAPER_STATUS_CHANGE_FAIL.getCode(), ex.getErrorCode());
    }
}
