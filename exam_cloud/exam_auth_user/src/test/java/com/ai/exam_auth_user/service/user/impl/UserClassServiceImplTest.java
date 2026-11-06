package com.ai.exam_auth_user.service.user.impl;

import com.ai.exam_auth_user.mapper.UserClassMapper;
import com.ai.exam_auth_user.pojo.dos.UserClass;
import com.ai.exam_auth_user.pojo.enums.UserClassRelationEnum;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserClassServiceImplTest {

    @Mock
    private UserClassMapper userClassMapper;

    @InjectMocks
    private UserClassServiceImpl userClassService;

    @BeforeAll
    static void initTableInfo() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, UserClass.class);
    }

    private static UserClass relationOf(Integer userId, Integer classId) {
        UserClass relation = new UserClass();
        relation.setUserId(userId);
        relation.setClassId(classId);
        relation.setRelationType(1);
        return relation;
    }

    @Test
    @DisplayName("listClassIds 返回去重后的班级ID并过滤空值")
    void listClassIds_distinctAndFiltersNull() {
        when(userClassMapper.selectList(any())).thenReturn(List.of(
                relationOf(7, 1), relationOf(7, 1), relationOf(7, 2), relationOf(7, null)));
        assertEquals(List.of(1, 2),
                userClassService.listClassIds(7, UserClassRelationEnum.TEACHER));
    }

    @Test
    @DisplayName("listClassIds 无关联记录时返回空列表")
    void listClassIds_emptyResult() {
        when(userClassMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertTrue(userClassService.listClassIds(7, UserClassRelationEnum.STUDENT).isEmpty());
    }

    @Test
    @DisplayName("listClassIds userId 为 null 抛 PARAM_ERROR")
    void listClassIds_nullUserId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userClassService.listClassIds(null, UserClassRelationEnum.STUDENT));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verifyNoInteractions(userClassMapper);
    }

    @Test
    @DisplayName("listClassIds relationType 为 null 抛 PARAM_ERROR")
    void listClassIds_nullRelationType() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userClassService.listClassIds(7, null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verifyNoInteractions(userClassMapper);
    }

    @Test
    @DisplayName("listClassIdsByUserIds 按用户ID分组")
    void listClassIdsByUserIds_groupsByUser() {
        when(userClassMapper.selectList(any())).thenReturn(List.of(
                relationOf(7, 1), relationOf(7, 2), relationOf(8, 3), relationOf(null, 4)));
        Map<Integer, List<Integer>> result = userClassService.listClassIdsByUserIds(
                List.of(7, 8), UserClassRelationEnum.STUDENT);
        assertEquals(List.of(1, 2), result.get(7));
        assertEquals(List.of(3), result.get(8));
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("listClassIdsByUserIds 空入参返回空Map且不查库")
    void listClassIdsByUserIds_emptyInput() {
        assertTrue(userClassService.listClassIdsByUserIds(null, UserClassRelationEnum.STUDENT).isEmpty());
        assertTrue(userClassService.listClassIdsByUserIds(
                Collections.emptyList(), UserClassRelationEnum.STUDENT).isEmpty());
        verifyNoInteractions(userClassMapper);
    }

    @Test
    @DisplayName("listClassIdsByUserIds relationType 为 null 抛 PARAM_ERROR")
    void listClassIdsByUserIds_nullRelationType() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userClassService.listClassIdsByUserIds(List.of(7), null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verifyNoInteractions(userClassMapper);
    }

    @Test
    @DisplayName("listUserIdsByClassId 返回去重后的用户ID")
    void listUserIdsByClassId_distinct() {
        when(userClassMapper.selectList(any())).thenReturn(List.of(
                relationOf(7, 1), relationOf(7, 1), relationOf(8, 1), relationOf(null, 1)));
        assertEquals(List.of(7, 8),
                userClassService.listUserIdsByClassId(1, UserClassRelationEnum.STUDENT));
    }

    @Test
    @DisplayName("listUserIdsByClassId classId 为 null 抛 PARAM_ERROR")
    void listUserIdsByClassId_nullClassId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userClassService.listUserIdsByClassId(null, UserClassRelationEnum.STUDENT));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verifyNoInteractions(userClassMapper);
    }

    @Test
    @DisplayName("countByClassId selectCount 返回 null 时按 0 处理")
    void countByClassId_nullCount() {
        when(userClassMapper.selectCount(any())).thenReturn(null);
        assertEquals(0L, userClassService.countByClassId(1));
    }

    @Test
    @DisplayName("countByClassId classId 为 null 抛 PARAM_ERROR")
    void countByClassId_nullClassId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userClassService.countByClassId(null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verifyNoInteractions(userClassMapper);
    }

    @Test
    @DisplayName("replaceClassIds 先删旧关联再按去重后的ID写入")
    void replaceClassIds_deletesThenInsertsDistinct() {
        when(userClassMapper.insert(any(UserClass.class))).thenReturn(1);
        userClassService.replaceClassIds(7, UserClassRelationEnum.TEACHER,
                Arrays.asList(1, 1, 2, null));

        verify(userClassMapper).delete(any());
        ArgumentCaptor<UserClass> captor = ArgumentCaptor.forClass(UserClass.class);
        verify(userClassMapper, times(2)).insert(captor.capture());
        assertEquals(List.of(1, 2),
                captor.getAllValues().stream().map(UserClass::getClassId).toList());
        assertEquals(Integer.valueOf(2), captor.getAllValues().get(0).getRelationType());
        assertEquals(Integer.valueOf(7), captor.getAllValues().get(0).getUserId());
    }

    @Test
    @DisplayName("replaceClassIds 空列表只清空旧关联不写入")
    void replaceClassIds_emptyListClearsRelation() {
        userClassService.replaceClassIds(7, UserClassRelationEnum.STUDENT,
                Collections.emptyList());
        verify(userClassMapper).delete(any());
        verify(userClassMapper, never()).insert(any(UserClass.class));
    }

    @Test
    @DisplayName("replaceClassIds 唯一键冲突转为 6004")
    void replaceClassIds_duplicateKey() {
        when(userClassMapper.insert(any(UserClass.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userClassService.replaceClassIds(7, UserClassRelationEnum.STUDENT,
                        List.of(1)));
        assertEquals(ErrorCodeEnum.STUDENT_CLASS_CONFLICT.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("replaceClassIds userId 为 null 抛 PARAM_ERROR 且不删旧关联")
    void replaceClassIds_nullUserId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userClassService.replaceClassIds(null, UserClassRelationEnum.STUDENT,
                        List.of(1)));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verifyNoInteractions(userClassMapper);
    }
}
