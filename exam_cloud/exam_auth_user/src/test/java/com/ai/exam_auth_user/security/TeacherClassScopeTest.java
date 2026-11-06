package com.ai.exam_auth_user.security;

import com.ai.exam_auth_user.pojo.enums.UserClassRelationEnum;
import com.ai.exam_auth_user.service.user.ISysClassService;
import com.ai.exam_auth_user.service.user.IUserClassService;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.security.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherClassScopeTest {

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private IUserClassService userClassService;

    @Mock
    private ISysClassService sysClassService;

    @InjectMocks
    private TeacherClassScope teacherClassScope;

    private void loginAs(String userId, Integer userType) {
        lenient().when(securityUtil.getCurrentUserId()).thenReturn(userId);
        lenient().when(securityUtil.getCurrentUserType()).thenReturn(userType);
    }

    @Test
    @DisplayName("管理员的班级范围不受限制")
    void scopedClassIds_adminIsUnlimited() {
        loginAs("1", 3);
        assertNull(teacherClassScope.scopedClassIds());
        verifyNoInteractions(userClassService, sysClassService);
    }

    @Test
    @DisplayName("教师按 TEACHER 关系查询任教班级并过滤停用班")
    void scopedClassIds_teacherReturnsOwnClasses() {
        loginAs("7", 2);
        when(userClassService.listClassIds(7, UserClassRelationEnum.TEACHER))
                .thenReturn(List.of(1, 2));
        when(sysClassService.filterActiveClassIds(List.of(1, 2))).thenReturn(List.of(1, 2));
        assertEquals(List.of(1, 2), teacherClassScope.scopedClassIds());
        verify(userClassService).listClassIds(7, UserClassRelationEnum.TEACHER);
        verify(sysClassService).filterActiveClassIds(List.of(1, 2));
    }

    @Test
    @DisplayName("启用班级查询返回 null 时按空范围处理")
    void scopedClassIds_nullActiveIdsBecomesEmpty() {
        loginAs("7", 2);
        when(userClassService.listClassIds(7, UserClassRelationEnum.TEACHER))
                .thenReturn(List.of(1, 2));
        when(sysClassService.filterActiveClassIds(List.of(1, 2))).thenReturn(null);
        assertEquals(List.of(), teacherClassScope.scopedClassIds());
    }

    @Test
    @DisplayName("userId 为 null 时抛用户未登录")
    void scopedClassIds_missingUserId() {
        loginAs(null, 2);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherClassScope.scopedClassIds());
        assertEquals(ErrorCodeEnum.USER_NOT_LOGIN.getCode(), ex.getErrorCode());
        verifyNoInteractions(userClassService, sysClassService);
    }

    @Test
    @DisplayName("userType 为 null 时抛用户未登录")
    void scopedClassIds_missingUserType() {
        loginAs("7", null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherClassScope.scopedClassIds());
        assertEquals(ErrorCodeEnum.USER_NOT_LOGIN.getCode(), ex.getErrorCode());
        verifyNoInteractions(userClassService, sysClassService);
    }

    @Test
    @DisplayName("管理员可查看任意班级")
    void assertClassVisible_adminAllowsAnyClass() {
        loginAs("1", 3);
        assertDoesNotThrow(() -> teacherClassScope.assertClassVisible(999));
        verifyNoInteractions(userClassService, sysClassService);
    }

    @Test
    @DisplayName("教师查看自己带的启用班放行")
    void assertClassVisible_teacherOwnClass() {
        loginAs("7", 2);
        when(userClassService.listClassIds(7, UserClassRelationEnum.TEACHER))
                .thenReturn(List.of(1, 2));
        when(sysClassService.filterActiveClassIds(List.of(1, 2))).thenReturn(List.of(1, 2));
        assertDoesNotThrow(() -> teacherClassScope.assertClassVisible(2));
    }

    @Test
    @DisplayName("教师查看别班抛无权限")
    void assertClassVisible_teacherForeignClass() {
        loginAs("7", 2);
        when(userClassService.listClassIds(7, UserClassRelationEnum.TEACHER))
                .thenReturn(List.of(1, 2));
        when(sysClassService.filterActiveClassIds(List.of(1, 2))).thenReturn(List.of(1, 2));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherClassScope.assertClassVisible(99));
        assertEquals(ErrorCodeEnum.PERMISSION_DENIED.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("停用班级被过滤后教师不可见")
    void assertClassVisible_disabledClassRejected() {
        loginAs("7", 2);
        when(userClassService.listClassIds(7, UserClassRelationEnum.TEACHER))
                .thenReturn(List.of(1, 2));
        when(sysClassService.filterActiveClassIds(List.of(1, 2))).thenReturn(List.of(1));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherClassScope.assertClassVisible(2));
        assertEquals(ErrorCodeEnum.PERMISSION_DENIED.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("classId 为 null 抛 PARAM_ERROR 且不读登录态")
    void assertClassVisible_nullClassId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> teacherClassScope.assertClassVisible(null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verifyNoInteractions(securityUtil, userClassService, sysClassService);
    }
}
