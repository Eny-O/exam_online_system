package com.ai.exam_spring.modules.exam.service.impl;

import com.ai.exam_api.feign.UserFeignClient;
import com.ai.exam_api.pojo.dto.UserBriefDTO;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.security.SecurityUtil;
import com.ai.exam_common.common.status.PaperStatus;
import com.ai.exam_spring.modules.exam.mapper.ExamManagementMapper;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamDetailAddDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamDetailUpdateDTO;
import com.ai.exam_spring.modules.exam.service.IExamClassService;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.paper.service.IPaperManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamManagementTeacherScopeTest {

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private IPaperManagementService paperManagementService;

    @Mock
    private ExamManagementMapper examManagementMapper;

    @Mock
    private IExamClassService examClassService;

    @Mock
    private IExamStudentService examStudentService;

    @InjectMocks
    private ExamManagementServiceImpl examManagementService;

    private static final Integer TEACHER_ID = 7;
    private static final Integer OWN_CLASS = 1;
    private static final Integer FOREIGN_CLASS = 9;
    private static final Integer EXAM_ID = 10;

    private void loginAs(String userId, Integer userType) {
        lenient().when(securityUtil.getCurrentUserId()).thenReturn(userId);
        lenient().when(securityUtil.getCurrentUserType()).thenReturn(userType);
    }

    private void allowPaper() {
        lenient().when(paperManagementService.getPaperStatusById(any()))
                .thenReturn(PaperStatus.ALREADY_ENABLE);
    }

    private void stubTeacherClasses(List<Integer> classIds) {
        when(userFeignClient.listTeacherClassIds(TEACHER_ID)).thenReturn(classIds);
    }

    private static ExamDetailAddDTO addDtoOf(List<Integer> classIds, List<Integer> studentIds) {
        ExamDetailAddDTO dto = new ExamDetailAddDTO();
        dto.setPaperId(100);
        dto.setClassIds(classIds);
        dto.setStudentIds(studentIds);
        return dto;
    }

    private static UserBriefDTO briefOf(Integer id, Integer classId) {
        UserBriefDTO brief = new UserBriefDTO();
        brief.setId(id);
        brief.setClassId(classId);
        return brief;
    }

    private void assertDenied(Runnable action) {
        BusinessException ex = assertThrows(BusinessException.class, action::run);
        assertEquals(ErrorCodeEnum.PERMISSION_DENIED.getCode(), ex.getErrorCode());
        verify(examManagementMapper, never()).insert(any(Exam.class));
    }

    // ===== 登录态与非教师 =====

    @Test
    @DisplayName("addExam 用户ID为空抛用户未登录")
    void addExam_missingUserId() {
        loginAs(null, 2);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.addExam(addDtoOf(null, null)));
        assertEquals(ErrorCodeEnum.USER_NOT_LOGIN.getCode(), ex.getErrorCode());
        verifyNoInteractions(userFeignClient);
        verify(examManagementMapper, never()).insert(any(Exam.class));
    }

    @Test
    @DisplayName("addExam 用户类型为空抛用户未登录")
    void addExam_missingUserType() {
        loginAs("7", null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.addExam(addDtoOf(null, null)));
        assertEquals(ErrorCodeEnum.USER_NOT_LOGIN.getCode(), ex.getErrorCode());
        verifyNoInteractions(userFeignClient);
        verify(examManagementMapper, never()).insert(any(Exam.class));
    }

    @Test
    @DisplayName("addExam 非教师不做任教范围校验")
    void addExam_nonTeacherSkipsScope() {
        loginAs("1", 3);
        allowPaper();

        examManagementService.addExam(addDtoOf(List.of(FOREIGN_CLASS), List.of(101)));

        verify(userFeignClient, never()).listTeacherClassIds(any());
        verify(examManagementMapper).insert(any(Exam.class));
    }

    // ===== 班级范围 =====

    @Test
    @DisplayName("addExam 教师选择非任教班级抛无权限")
    void addExam_teacherForeignClass() {
        loginAs("7", 2);
        stubTeacherClasses(List.of(OWN_CLASS, 2));

        assertDenied(() -> examManagementService.addExam(
                addDtoOf(List.of(OWN_CLASS, FOREIGN_CLASS), null)));
        verify(userFeignClient, never()).listUserBriefs(anyList());
    }

    @Test
    @DisplayName("addExam 任教班级查询返回 null 时拒绝选班")
    void addExam_teacherNullClassScope() {
        loginAs("7", 2);
        stubTeacherClasses(null);

        assertDenied(() -> examManagementService.addExam(
                addDtoOf(List.of(OWN_CLASS), null)));
    }

    @Test
    @DisplayName("addExam 教师选择任教班级放行")
    void addExam_teacherOwnClass() {
        loginAs("7", 2);
        stubTeacherClasses(List.of(OWN_CLASS, 2));
        allowPaper();

        examManagementService.addExam(addDtoOf(List.of(OWN_CLASS, 2), null));

        verify(examManagementMapper).insert(any(Exam.class));
        verify(userFeignClient, never()).listUserBriefs(anyList());
    }

    @Test
    @DisplayName("addExam 班级与学生列表为空时放行且不查用户摘要")
    void addExam_noScopeRequest() {
        loginAs("7", 2);
        stubTeacherClasses(List.of(OWN_CLASS));
        allowPaper();

        examManagementService.addExam(addDtoOf(null, null));

        verify(examManagementMapper).insert(any(Exam.class));
        verify(userFeignClient, never()).listUserBriefs(anyList());
    }

    // ===== 学生范围 =====

    @Test
    @DisplayName("addExam 教师选择别班学生抛无权限")
    void addExam_teacherForeignStudent() {
        loginAs("7", 2);
        stubTeacherClasses(List.of(OWN_CLASS));
        when(userFeignClient.listUserBriefs(anyList()))
                .thenReturn(List.of(briefOf(101, FOREIGN_CLASS)));

        assertDenied(() -> examManagementService.addExam(
                addDtoOf(null, List.of(101))));
    }

    @Test
    @DisplayName("addExam 用户摘要缺失所请求学生时抛无权限")
    void addExam_teacherMissingStudentBrief() {
        loginAs("7", 2);
        stubTeacherClasses(List.of(OWN_CLASS));
        when(userFeignClient.listUserBriefs(anyList()))
                .thenReturn(List.of(briefOf(999, OWN_CLASS)));

        assertDenied(() -> examManagementService.addExam(
                addDtoOf(null, List.of(101))));
    }

    @Test
    @DisplayName("addExam 用户摘要返回 null 时抛无权限")
    void addExam_teacherNullStudentBriefs() {
        loginAs("7", 2);
        stubTeacherClasses(List.of(OWN_CLASS));
        when(userFeignClient.listUserBriefs(anyList())).thenReturn(null);

        assertDenied(() -> examManagementService.addExam(
                addDtoOf(null, List.of(101))));
    }

    @Test
    @DisplayName("addExam 学生无归属班级时抛无权限")
    void addExam_teacherStudentWithoutClass() {
        loginAs("7", 2);
        stubTeacherClasses(List.of(OWN_CLASS));
        when(userFeignClient.listUserBriefs(anyList()))
                .thenReturn(List.of(briefOf(101, null)));

        assertDenied(() -> examManagementService.addExam(
                addDtoOf(null, List.of(101))));
    }

    @Test
    @DisplayName("addExam 教师选择任教班级内学生放行")
    void addExam_teacherOwnStudents() {
        loginAs("7", 2);
        stubTeacherClasses(List.of(OWN_CLASS));
        when(userFeignClient.listUserBriefs(anyList()))
                .thenReturn(List.of(briefOf(101, OWN_CLASS)));
        allowPaper();

        examManagementService.addExam(addDtoOf(List.of(OWN_CLASS), List.of(101)));

        verify(examManagementMapper).insert(any(Exam.class));
    }

    @Test
    @DisplayName("addExam 关联ID中的空值被过滤后放行")
    void addExam_nullIdElements() {
        loginAs("7", 2);
        stubTeacherClasses(Arrays.asList(OWN_CLASS, null));
        when(userFeignClient.listUserBriefs(anyList()))
                .thenReturn(List.of(briefOf(101, OWN_CLASS)));
        allowPaper();

        examManagementService.addExam(addDtoOf(
                Arrays.asList(OWN_CLASS, null), Arrays.asList(101, null)));

        verify(examManagementMapper).insert(any(Exam.class));
    }

    @Test
    @DisplayName("addExam 任教班级远程查询失败时不写入考试")
    void addExam_teacherScopeFeignFailure() {
        loginAs("7", 2);
        when(userFeignClient.listTeacherClassIds(TEACHER_ID))
                .thenThrow(new IllegalStateException("timeout"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> examManagementService.addExam(addDtoOf(List.of(OWN_CLASS), null)));
        assertEquals("timeout", ex.getMessage());
        verify(examManagementMapper, never()).insert(any(Exam.class));
    }

    // ===== updateExam 复用同一校验 =====

    @Test
    @DisplayName("updateExam 教师选择非任教班级抛无权限且不继续更新")
    void updateExam_teacherForeignClass() {
        loginAs("7", 2);
        stubTeacherClasses(List.of(OWN_CLASS));
        when(examManagementMapper.selectById(EXAM_ID)).thenReturn(new Exam());
        ExamDetailUpdateDTO dto = new ExamDetailUpdateDTO();
        dto.setId(EXAM_ID);
        dto.setClassIds(List.of(FOREIGN_CLASS));
        dto.setStudentIds(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examManagementService.updateExam(dto));
        assertEquals(ErrorCodeEnum.PERMISSION_DENIED.getCode(), ex.getErrorCode());
        verify(examManagementMapper, never()).updateById(any(Exam.class));
        verifyNoInteractions(examStudentService, examClassService);
    }
}
