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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
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
        verifyNoInteractions(examClassFeignClient);
    }

    @Test
    @DisplayName("deleteClass 本库存在师生关联时抛 6003 且不发起远程校验")
    void deleteClass_hasMembers() {
        when(sysClassMapper.selectById(CLASS_ID)).thenReturn(classOf(CLASS_ID, "一班"));
        when(userClassService.countByClassId(CLASS_ID)).thenReturn(2L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.deleteClass(CLASS_ID));
        assertEquals(ErrorCodeEnum.CLASS_HAS_MEMBERS.getCode(), ex.getErrorCode());
        verifyNoInteractions(examClassFeignClient);
        verify(sysClassService, never()).removeById(CLASS_ID);
    }

    @Test
    @DisplayName("deleteClass 远程校验异常时拒绝删除")
    void deleteClass_feignFailureRejects() {
        stubClassExistsWithoutMembers();
        when(examClassFeignClient.hasClassReference(CLASS_ID))
                .thenThrow(new RuntimeException("timeout"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.deleteClass(CLASS_ID));
        assertEquals(ErrorCodeEnum.CLASS_REF_CHECK_FAILED.getCode(), ex.getErrorCode());
        verify(sysClassService, never()).removeById(CLASS_ID);
    }

    @Test
    @DisplayName("deleteClass 远程校验返回 null 时拒绝删除")
    void deleteClass_feignNullResultRejects() {
        stubClassExistsWithoutMembers();
        when(examClassFeignClient.hasClassReference(CLASS_ID)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.deleteClass(CLASS_ID));
        assertEquals(ErrorCodeEnum.CLASS_REF_CHECK_FAILED.getCode(), ex.getErrorCode());
        verify(sysClassService, never()).removeById(CLASS_ID);
    }

    @Test
    @DisplayName("deleteClass 远程校验返回空 data 时拒绝删除")
    void deleteClass_feignNullDataRejects() {
        stubClassExistsWithoutMembers();
        when(examClassFeignClient.hasClassReference(CLASS_ID))
                .thenReturn(ResponseResult.<Boolean>success("ok", null));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.deleteClass(CLASS_ID));
        assertEquals(ErrorCodeEnum.CLASS_REF_CHECK_FAILED.getCode(), ex.getErrorCode());
        verify(sysClassService, never()).removeById(CLASS_ID);
    }

    @Test
    @DisplayName("deleteClass 考试引用该班级时抛 6005")
    void deleteClass_referencedByExam() {
        stubClassExistsWithoutMembers();
        when(examClassFeignClient.hasClassReference(CLASS_ID))
                .thenReturn(ResponseResult.success(true));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.deleteClass(CLASS_ID));
        assertEquals(ErrorCodeEnum.CLASS_REFERENCED_BY_EXAM.getCode(), ex.getErrorCode());
        verify(sysClassService, never()).removeById(CLASS_ID);
    }

    @Test
    @DisplayName("deleteClass 无引用时正常删除")
    void deleteClass_success() {
        stubClassExistsWithoutMembers();
        when(examClassFeignClient.hasClassReference(CLASS_ID))
                .thenReturn(ResponseResult.success(false));
        doReturn(true).when(sysClassService).removeById(CLASS_ID);
        sysClassService.deleteClass(CLASS_ID);
        verify(sysClassService).removeById(CLASS_ID);
    }

    @Test
    @DisplayName("deleteClass 删除时触发数据完整性异常转 6003")
    void deleteClass_integrityViolation() {
        stubClassExistsWithoutMembers();
        when(examClassFeignClient.hasClassReference(CLASS_ID))
                .thenReturn(ResponseResult.success(false));
        doThrow(new DataIntegrityViolationException("foreign key"))
                .when(sysClassService).removeById(CLASS_ID);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysClassService.deleteClass(CLASS_ID));
        assertEquals(ErrorCodeEnum.CLASS_HAS_MEMBERS.getCode(), ex.getErrorCode());
    }
}
