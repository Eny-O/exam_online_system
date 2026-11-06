package com.ai.exam_spring.modules.exam.service.impl;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_spring.modules.exam.mapper.ExamClassMapper;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamClass;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamClassServiceImplTest {

    @Mock
    private ExamClassMapper examClassMapper;

    private ExamClassServiceImpl examClassService;

    private static final Integer CLASS_ID = 5;

    @BeforeAll
    static void initTableInfo() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, ExamClass.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        examClassService = spy(new ExamClassServiceImpl());
        Class<?> clazz = examClassService.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField("baseMapper");
                field.setAccessible(true);
                field.set(examClassService, examClassMapper);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("baseMapper");
    }

    @Test
    @DisplayName("hasClassReference classId 为 null 抛 PARAM_ERROR 且不查库")
    void hasClassReference_nullClassId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> examClassService.hasClassReference(null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verifyNoInteractions(examClassMapper);
    }

    @Test
    @DisplayName("hasClassReference 存在考试班级关联时返回 true")
    void hasClassReference_referenced() {
        when(examClassMapper.selectCount(any())).thenReturn(2L);
        assertTrue(examClassService.hasClassReference(CLASS_ID));
    }

    @Test
    @DisplayName("hasClassReference 无考试班级关联时返回 false")
    void hasClassReference_notReferenced() {
        when(examClassMapper.selectCount(any())).thenReturn(0L);
        assertFalse(examClassService.hasClassReference(CLASS_ID));
    }

    @Test
    @DisplayName("hasClassReference selectCount 返回 null 时按无引用处理")
    void hasClassReference_nullCount() {
        when(examClassMapper.selectCount(any())).thenReturn(null);
        assertFalse(examClassService.hasClassReference(CLASS_ID));
    }

    @Test
    @DisplayName("hasClassReference 按 classId 过滤而不是全表统计")
    void hasClassReference_filtersByClassId() {
        when(examClassMapper.selectCount(any())).thenReturn(0L);

        examClassService.hasClassReference(CLASS_ID);

        verify(examClassMapper).selectCount(argThat(wrapper ->
                wrapper.getSqlSegment().contains("class_id")));
    }

    @Test
    @DisplayName("hasClassReference 查询异常直接上抛")
    void hasClassReference_queryFailure() {
        when(examClassMapper.selectCount(any())).thenThrow(new IllegalStateException("db down"));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> examClassService.hasClassReference(CLASS_ID));
        assertEquals("db down", ex.getMessage());
    }
}
