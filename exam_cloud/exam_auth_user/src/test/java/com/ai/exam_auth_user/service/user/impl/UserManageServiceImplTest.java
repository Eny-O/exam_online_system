package com.ai.exam_auth_user.service.user.impl;

import com.ai.exam_auth_user.mapper.UserManageMapper;
import com.ai.exam_auth_user.mapper.UserMapper;
import com.ai.exam_auth_user.pojo.dos.User;
import com.ai.exam_auth_user.pojo.dto.UserAddDTO;
import com.ai.exam_auth_user.pojo.dto.UserQueryDTO;
import com.ai.exam_auth_user.pojo.dto.UserUpdateDTO;
import com.ai.exam_auth_user.pojo.enums.UserClassRelationEnum;
import com.ai.exam_auth_user.pojo.vo.UserManageVO;
import com.ai.exam_auth_user.service.user.IUserClassService;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManageServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserManageMapper userManageMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IUserClassService userClassService;

    private UserManageServiceImpl userManageService;

    private static final Integer USER_ID = 7;

    @BeforeAll
    static void initTableInfo() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        userManageService = spy(new UserManageServiceImpl());
        setField(userManageService, "baseMapper", userMapper);
        setField(userManageService, "userManageMapper", userManageMapper);
        setField(userManageService, "passwordEncoder", passwordEncoder);
        setField(userManageService, "userClassService", userClassService);
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

    private static UserAddDTO addDtoOf(Integer userType) {
        UserAddDTO dto = new UserAddDTO();
        dto.setUsername("test01");
        dto.setPassword("pwd");
        dto.setRealName("张三");
        dto.setPhone("13800000000");
        dto.setUserType(userType);
        return dto;
    }

    private static UserUpdateDTO updateDtoOf(Integer id) {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setId(id);
        return dto;
    }

    private static User userOf(Integer userType) {
        User user = new User();
        user.setId(USER_ID);
        user.setUserType(userType);
        user.setStatus(1);
        return user;
    }

    private static UserManageVO voOf(Integer id, Integer userType) {
        UserManageVO vo = new UserManageVO();
        vo.setId(id);
        vo.setUserType(userType);
        return vo;
    }

    private void stubInsertAssignId() {
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(USER_ID);
            return 1;
        });
    }

    private void stubClassIdMaps(Map<Integer, List<Integer>> studentClasses,
                                 Map<Integer, List<Integer>> teacherClasses) {
        lenient().when(userClassService.listClassIdsByUserIds(any(), eq(UserClassRelationEnum.STUDENT)))
                .thenReturn(studentClasses);
        lenient().when(userClassService.listClassIdsByUserIds(any(), eq(UserClassRelationEnum.TEACHER)))
                .thenReturn(teacherClasses);
    }

    private Page<UserManageVO> pageOf(List<UserManageVO> records) {
        Page<UserManageVO> page = new Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(records.size());
        return page;
    }

    // ===== addUser =====

    @Test
    @DisplayName("addUser 用户名已存在抛 1002 且不写用户与班级关联")
    void addUser_duplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userManageService.addUser(addDtoOf(1)));
        assertEquals(ErrorCodeEnum.USER_ALREADY_EXISTS.getCode(), ex.getErrorCode());
        verify(userMapper, never()).insert(any(User.class));
        verifyNoInteractions(userClassService);
    }

    @Test
    @DisplayName("addUser 学生按单值 classId 写入 STUDENT 关联并加密密码")
    void addUser_studentSingleClass() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("pwd")).thenReturn("encoded");
        stubInsertAssignId();
        UserAddDTO dto = addDtoOf(1);
        dto.setClassId(11);
        dto.setClassIds(List.of(99));

        userManageService.addUser(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("encoded", captor.getValue().getPassword());
        assertEquals(Integer.valueOf(1), captor.getValue().getStatus());
        verify(userClassService).replaceClassIds(USER_ID,
                UserClassRelationEnum.STUDENT, List.of(11));
    }

    @Test
    @DisplayName("addUser 学生 classId 为 null 时写入空关联")
    void addUser_studentNullClass() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        stubInsertAssignId();
        UserAddDTO dto = addDtoOf(1);
        dto.setClassIds(List.of(99));

        userManageService.addUser(dto);

        verify(userClassService).replaceClassIds(USER_ID,
                UserClassRelationEnum.STUDENT, Collections.emptyList());
    }

    @Test
    @DisplayName("addUser 教师按 classIds 写入 TEACHER 关联")
    void addUser_teacherMultiClass() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        stubInsertAssignId();
        UserAddDTO dto = addDtoOf(2);
        dto.setClassId(99);
        dto.setClassIds(List.of(1, 2));

        userManageService.addUser(dto);

        verify(userClassService).replaceClassIds(USER_ID,
                UserClassRelationEnum.TEACHER, List.of(1, 2));
    }

    @Test
    @DisplayName("addUser 教师 classIds 为 null 时写入空关联")
    void addUser_teacherNullClassIds() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        stubInsertAssignId();

        userManageService.addUser(addDtoOf(2));

        verify(userClassService).replaceClassIds(USER_ID,
                UserClassRelationEnum.TEACHER, Collections.emptyList());
    }

    @Test
    @DisplayName("addUser 管理员不写入班级关联")
    void addUser_adminSkipsRelation() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        stubInsertAssignId();

        userManageService.addUser(addDtoOf(3));

        verify(userMapper).insert(any(User.class));
        verifyNoInteractions(userClassService);
    }

    // ===== updateUser =====

    @Test
    @DisplayName("updateUser 用户不存在抛 1001 且不写班级关联")
    void updateUser_notFound() {
        when(userMapper.selectById(USER_ID)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userManageService.updateUser(updateDtoOf(USER_ID)));
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), ex.getErrorCode());
        verifyNoInteractions(userClassService);
    }

    @Test
    @DisplayName("updateUser 学生按单值 classId 覆盖 STUDENT 关联")
    void updateUser_studentClassId() {
        when(userMapper.selectById(USER_ID)).thenReturn(userOf(1));
        UserUpdateDTO dto = updateDtoOf(USER_ID);
        dto.setClassId(12);
        dto.setClassIds(List.of(99));

        userManageService.updateUser(dto);

        verify(userClassService).replaceClassIds(USER_ID,
                UserClassRelationEnum.STUDENT, List.of(12));
    }

    @Test
    @DisplayName("updateUser 学生 classId 为 null 时清空 STUDENT 关联")
    void updateUser_studentNullClassId() {
        when(userMapper.selectById(USER_ID)).thenReturn(userOf(1));
        UserUpdateDTO dto = updateDtoOf(USER_ID);
        dto.setClassIds(List.of(99));

        userManageService.updateUser(dto);

        verify(userClassService).replaceClassIds(USER_ID,
                UserClassRelationEnum.STUDENT, Collections.emptyList());
    }

    @Test
    @DisplayName("updateUser 教师未传 classIds 时保留原关联")
    void updateUser_teacherNullClassIds() {
        when(userMapper.selectById(USER_ID)).thenReturn(userOf(2));

        userManageService.updateUser(updateDtoOf(USER_ID));

        verify(userMapper).updateById(any(User.class));
        verifyNoInteractions(userClassService);
    }

    @Test
    @DisplayName("updateUser 教师传入 classIds 时覆盖 TEACHER 关联")
    void updateUser_teacherClassIds() {
        when(userMapper.selectById(USER_ID)).thenReturn(userOf(2));
        UserUpdateDTO dto = updateDtoOf(USER_ID);
        dto.setClassIds(List.of(1, 3));

        userManageService.updateUser(dto);

        verify(userClassService).replaceClassIds(USER_ID,
                UserClassRelationEnum.TEACHER, List.of(1, 3));
    }

    @Test
    @DisplayName("updateUser 管理员不写入班级关联")
    void updateUser_adminSkipsRelation() {
        when(userMapper.selectById(USER_ID)).thenReturn(userOf(3));

        userManageService.updateUser(updateDtoOf(USER_ID));

        verify(userMapper).updateById(any(User.class));
        verifyNoInteractions(userClassService);
    }

    // ===== pageUser =====

    @Test
    @DisplayName("pageUser 按用户类型补齐 classIds，缺失关联补空列表")
    void pageUser_fillsClassIds() {
        UserManageVO student = voOf(1, 1);
        UserManageVO teacher = voOf(2, 2);
        UserManageVO admin = voOf(3, 3);
        when(userManageMapper.pageUserManage(any(), any(), any(), any(), any(), any()))
                .thenReturn(pageOf(List.of(student, teacher, admin)));
        stubClassIdMaps(Map.of(1, List.of(11)), Map.of(2, List.of(21, 22)));

        PageResult<UserManageVO> result = userManageService.pageUser(new UserQueryDTO());

        assertEquals(List.of(11), result.getRecords().get(0).getClassIds());
        assertEquals(List.of(21, 22), result.getRecords().get(1).getClassIds());
        assertTrue(result.getRecords().get(2).getClassIds().isEmpty());
    }

    @Test
    @DisplayName("pageUser 无记录时不查询班级关联")
    void pageUser_emptyRecords() {
        when(userManageMapper.pageUserManage(any(), any(), any(), any(), any(), any()))
                .thenReturn(pageOf(Collections.emptyList()));

        userManageService.pageUser(new UserQueryDTO());

        verifyNoInteractions(userClassService);
    }

    // ===== toggleStatus / resetPassword =====

    @Test
    @DisplayName("toggleStatus 在启用与禁用之间切换")
    void toggleStatus_switches() {
        when(userMapper.selectById(USER_ID)).thenReturn(userOf(1));
        userManageService.toggleStatus(USER_ID);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(Integer.valueOf(2), captor.getValue().getStatus());
    }

    @Test
    @DisplayName("toggleStatus 用户不存在抛 1001")
    void toggleStatus_notFound() {
        when(userMapper.selectById(USER_ID)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userManageService.toggleStatus(USER_ID));
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), ex.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("resetPassword 写入加密后的默认密码")
    void resetPassword_encodesDefaultPassword() {
        when(userMapper.selectById(USER_ID)).thenReturn(userOf(1));
        when(passwordEncoder.encode("123456")).thenReturn("defaultEncoded");
        userManageService.resetPassword(USER_ID);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("defaultEncoded", captor.getValue().getPassword());
    }

    @Test
    @DisplayName("resetPassword 用户不存在抛 1001")
    void resetPassword_notFound() {
        when(userMapper.selectById(USER_ID)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userManageService.resetPassword(USER_ID));
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), ex.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
    }
}
