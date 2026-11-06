package com.ai.exam_auth_user.service.user.impl;

import com.ai.exam_api.feign.ExamClassFeignClient;
import com.ai.exam_auth_user.mapper.SysClassMapper;
import com.ai.exam_auth_user.pojo.dos.SysClass;
import com.ai.exam_auth_user.pojo.dto.ClassAddDTO;
import com.ai.exam_auth_user.pojo.dto.ClassUpdateDTO;
import com.ai.exam_auth_user.service.user.IUserClassService;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysClassServiceImplTest {

    @Mock
    private SysClassMapper sysClassMapper;

    @Mock
    private IUserClassService userClassService;

    @Mock
    private ExamClassFeignClient examClassFeignClient;

    private SysClassServiceImpl sysClassService;

    private static final Integer CLASS_ID = 1;

    @BeforeAll
    static void initTableInfo() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SysClass.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        sysClassService = spy(new SysClassServiceImpl());
        setField(sysClassService, "baseMapper", sysClassMapper);
        setField(sysClassService, "userClassService", userClassService);
        setField(sysClassService, "examClassFeignClient", examClassFeignClient);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static SysClass classOf(Integer id, String className) {
        SysClass entity = new SysClass();
        entity.setId(id);
        entity.setClassName(className);
        entity.setDeleted(0);
        return entity;
    }

    private static SysClass disabledClassOf(Integer id, String className) {
        SysClass entity = classOf(id, className);
        entity.setDeleted(id);
        return entity;
    }

    private static ClassAddDTO addDtoOf(String className, String grade) {
        ClassAddDTO dto = new ClassAddDTO();
        dto.setClassName(className);
        dto.setGrade(grade);
        return dto;
    }

    private static ClassUpdateDTO updateDtoOf(Integer id, String className, String grade) {
        ClassUpdateDTO dto = new ClassUpdateDTO();
        dto.setId(id);
        dto.setClassName(className);
        dto.setGrade(grade);
        return dto;
    }

    private void stubClassExistsWithoutMembers() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(classOf(CLASS_ID, "一班"));
        when(userClassService.countByClassId(CLASS_ID)).thenReturn(0L);
    }

    private void stubUpdate() {
        doReturn(true).when(sysClassService).updateById(any(SysClass.class));
    }

    @Test
    @DisplayName("addClass 同名班级抛 6002 且不写入")
    void addClass_duplicateName() {
        when(sysClassMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.addClass(addDtoOf("一班", null)));
        assertEquals(ErrorCodeEnum.CLASS_NAME_DUPLICATE.getCode(), ex.getErrorCode());
        verify(sysClassMapper, never()).insert(any(SysClass.class));
    }

    @Test
    @DisplayName("addClass 去除名称首尾空格并把空白年级归一为 null")
    void addClass_normalizesFields() {
        when(sysClassMapper.selectCount(any())).thenReturn(0L);
        when(sysClassMapper.insert(any(SysClass.class))).thenReturn(1);
        sysClassService.addClass(addDtoOf("  一班  ", "   "));
        ArgumentCaptor<SysClass> captor = ArgumentCaptor.forClass(SysClass.class);
        verify(sysClassMapper).insert(captor.capture());
        assertEquals("一班", captor.getValue().getClassName());
        assertNull(captor.getValue().getGrade());
    }

    @Test
    @DisplayName("addClass 并发重名触发唯一键冲突时转 6002")
    void addClass_duplicateKey() {
        when(sysClassMapper.selectCount(any())).thenReturn(0L);
        when(sysClassMapper.insert(any(SysClass.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.addClass(addDtoOf("一班", null)));
        assertEquals(ErrorCodeEnum.CLASS_NAME_DUPLICATE.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("updateClass 班级不存在抛 6001")
    void updateClass_notFound() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.updateClass(updateDtoOf(CLASS_ID, "新名", null)));
        assertEquals(ErrorCodeEnum.CLASS_NOT_FOUND.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("updateClass 排除自身查重后写入归一化字段")
    void updateClass_excludesSelfAndNormalizes() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(classOf(CLASS_ID, "旧名"));
        when(sysClassMapper.selectCount(any())).thenReturn(0L);
        when(sysClassMapper.updateById(any(SysClass.class))).thenReturn(1);
        sysClassService.updateClass(updateDtoOf(CLASS_ID, " 新名 ", " 2026级 "));
        ArgumentCaptor<SysClass> captor = ArgumentCaptor.forClass(SysClass.class);
        verify(sysClassMapper).updateById(captor.capture());
        assertEquals(CLASS_ID, captor.getValue().getId());
        assertEquals("新名", captor.getValue().getClassName());
        assertEquals("2026级", captor.getValue().getGrade());
    }

    @Test
    @DisplayName("updateClass 改成别的班级已用名称抛 6002")
    void updateClass_duplicateName() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(classOf(CLASS_ID, "旧名"));
        when(sysClassMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.updateClass(updateDtoOf(CLASS_ID, "二班", null)));
        assertEquals(ErrorCodeEnum.CLASS_NAME_DUPLICATE.getCode(), ex.getErrorCode());
        verify(sysClassMapper, never()).updateById(any(SysClass.class));
    }

    @Test
    @DisplayName("查重只统计启用班级")
    void countByName_onlyCountsEnabledClasses() {
        when(sysClassMapper.selectCount(any())).thenReturn(0L);
        when(sysClassMapper.insert(any(SysClass.class))).thenReturn(1);
        sysClassService.addClass(addDtoOf("一班", null));
        verify(sysClassMapper).selectCount(argThat((Wrapper<SysClass> w) ->
                w.getCustomSqlSegment().contains("deleted")));
    }

    @Test
    @DisplayName("deleteClass id 为 null 抛 PARAM_ERROR")
    void deleteClass_nullId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.deleteClass(null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verifyNoInteractions(sysClassMapper, userClassService, examClassFeignClient);
    }

    @Test
    @DisplayName("deleteClass 班级不存在抛 6001")
    void deleteClass_notFound() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.deleteClass(CLASS_ID));
        assertEquals(ErrorCodeEnum.CLASS_NOT_FOUND.getCode(), ex.getErrorCode());
        verifyNoInteractions(userClassService, examClassFeignClient);
    }

    @Test
    @DisplayName("deleteClass 已停用班级幂等返回且不再写库")
    void deleteClass_alreadyDisabledIsIdempotent() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(disabledClassOf(CLASS_ID, "一班"));
        sysClassService.deleteClass(CLASS_ID);
        verify(sysClassService, never()).updateById(any(SysClass.class));
        verify(sysClassService, never()).removeById(CLASS_ID);
        verifyNoInteractions(userClassService, examClassFeignClient);
    }

    @Test
    @DisplayName("deleteClass 写 deleted=id 而不是物理删除")
    void deleteClass_writesDeletedIdInsteadOfPhysicalDelete() {
        stubClassExistsWithoutMembers();
        when(examClassFeignClient.hasClassReference(CLASS_ID))
                .thenReturn(ResponseResult.success(false));
        stubUpdate();
        sysClassService.deleteClass(CLASS_ID);
        ArgumentCaptor<SysClass> captor = ArgumentCaptor.forClass(SysClass.class);
        verify(sysClassService).updateById(captor.capture());
        assertEquals(CLASS_ID, captor.getValue().getId());
        assertEquals(CLASS_ID, captor.getValue().getDeleted());
        verify(sysClassService, never()).removeById(CLASS_ID);
    }

    @Test
    @DisplayName("deleteClass 本库师生关联不阻断停用")
    void deleteClass_membersDoNotBlock() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(classOf(CLASS_ID, "一班"));
        when(userClassService.countByClassId(CLASS_ID)).thenReturn(2L);
        when(examClassFeignClient.hasClassReference(CLASS_ID))
                .thenReturn(ResponseResult.success(false));
        stubUpdate();
        assertDoesNotThrow(() -> sysClassService.deleteClass(CLASS_ID));
        verify(sysClassService).updateById(any(SysClass.class));
    }

    @Test
    @DisplayName("deleteClass 远程校验异常不阻断停用")
    void deleteClass_feignFailureDoesNotBlock() {
        stubClassExistsWithoutMembers();
        when(examClassFeignClient.hasClassReference(CLASS_ID))
                .thenThrow(new RuntimeException("timeout"));
        stubUpdate();
        assertDoesNotThrow(() -> sysClassService.deleteClass(CLASS_ID));
        verify(sysClassService).updateById(any(SysClass.class));
    }

    @Test
    @DisplayName("deleteClass 远程校验返回 null 不阻断停用")
    void deleteClass_feignNullResultDoesNotBlock() {
        stubClassExistsWithoutMembers();
        when(examClassFeignClient.hasClassReference(CLASS_ID)).thenReturn(null);
        stubUpdate();
        assertDoesNotThrow(() -> sysClassService.deleteClass(CLASS_ID));
        verify(sysClassService).updateById(any(SysClass.class));
    }

    @Test
    @DisplayName("deleteClass 考试引用该班级不阻断停用")
    void deleteClass_referencedByExamDoesNotBlock() {
        stubClassExistsWithoutMembers();
        when(examClassFeignClient.hasClassReference(CLASS_ID))
                .thenReturn(ResponseResult.success(true));
        stubUpdate();
        assertDoesNotThrow(() -> sysClassService.deleteClass(CLASS_ID));
        verify(sysClassService).updateById(any(SysClass.class));
    }

    @Test
    @DisplayName("restoreClass id 为 null 抛 PARAM_ERROR")
    void restoreClass_nullId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.restoreClass(null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verifyNoInteractions(sysClassMapper, userClassService, examClassFeignClient);
    }

    @Test
    @DisplayName("restoreClass 班级不存在抛 6001")
    void restoreClass_notFound() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.restoreClass(CLASS_ID));
        assertEquals(ErrorCodeEnum.CLASS_NOT_FOUND.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("restoreClass 启用班级幂等返回")
    void restoreClass_activeIsIdempotent() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(classOf(CLASS_ID, "一班"));
        sysClassService.restoreClass(CLASS_ID);
        verify(sysClassService, never()).updateById(any(SysClass.class));
        verify(sysClassMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("restoreClass 已有同名启用班级时抛 6002")
    void restoreClass_duplicateActiveNameRejects() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(disabledClassOf(CLASS_ID, "一班"));
        when(sysClassMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.restoreClass(CLASS_ID));
        assertEquals(ErrorCodeEnum.CLASS_NAME_DUPLICATE.getCode(), ex.getErrorCode());
        verify(sysClassService, never()).updateById(any(SysClass.class));
    }

    @Test
    @DisplayName("restoreClass 无同名启用班级时把 deleted 置回 0")
    void restoreClass_resetsDeletedToZero() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(disabledClassOf(CLASS_ID, "一班"));
        when(sysClassMapper.selectCount(any())).thenReturn(0L);
        stubUpdate();
        sysClassService.restoreClass(CLASS_ID);
        ArgumentCaptor<SysClass> captor = ArgumentCaptor.forClass(SysClass.class);
        verify(sysClassService).updateById(captor.capture());
        assertEquals(CLASS_ID, captor.getValue().getId());
        assertEquals(Integer.valueOf(0), captor.getValue().getDeleted());
    }

    @Test
    @DisplayName("filterActiveClassIds 空入参返回空列表且不查库")
    void filterActiveClassIds_emptyInput() {
        assertEquals(Collections.emptyList(), sysClassService.filterActiveClassIds(null));
        assertEquals(Collections.emptyList(), sysClassService.filterActiveClassIds(List.of()));
        verifyNoInteractions(sysClassMapper);
    }

    @Test
    @DisplayName("filterActiveClassIds 过滤 null 后只返回启用班级")
    void filterActiveClassIds_returnsOnlyEnabled() {
        when(sysClassMapper.selectList(any())).thenReturn(List.of(classOf(1, "一班")));
        assertEquals(List.of(1), sysClassService.filterActiveClassIds(Arrays.asList(1, 2, null)));
        verify(sysClassMapper).selectList(argThat((Wrapper<SysClass> w) ->
                w.getCustomSqlSegment().contains("deleted")));
    }

    @Test
    @DisplayName("filterActiveClassIds 查询无结果时返回空列表")
    void filterActiveClassIds_noEnabledClass() {
        when(sysClassMapper.selectList(any())).thenReturn(null);
        assertEquals(Collections.emptyList(), sysClassService.filterActiveClassIds(List.of(1, 2)));
    }
}
