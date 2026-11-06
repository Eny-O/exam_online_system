package com.ai.exam_auth_user.service.user.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_auth_user.pojo.dto.UserAddDTO;
import com.ai.exam_auth_user.pojo.dto.UserQueryDTO;
import com.ai.exam_auth_user.pojo.dto.UserUpdateDTO;
import com.ai.exam_auth_user.pojo.dos.User;
import com.ai.exam_auth_user.pojo.enums.UserClassRelationEnum;
import com.ai.exam_auth_user.pojo.vo.UserManageVO;
import com.ai.exam_auth_user.mapper.UserManageMapper;
import com.ai.exam_auth_user.mapper.UserMapper;
import com.ai.exam_auth_user.service.user.IUserClassService;
import com.ai.exam_auth_user.service.user.IUserManageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class UserManageServiceImpl extends ServiceImpl<UserMapper, User> implements IUserManageService {

    @Autowired
    private UserManageMapper userManageMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IUserClassService userClassService;

    @Override
    public PageResult<UserManageVO> pageUser(UserQueryDTO dto) {
        Page<UserManageVO> page = Page.of(dto.getPageNum(), dto.getPageSize());
        Page<UserManageVO> result = userManageMapper.pageUserManage(
                page, dto.getRealName(), dto.getUserId(),
                dto.getUserType(), dto.getStatus(), dto.getClassId()
        );
        fillClassIds(result.getRecords());
        return PageResult.of(result.getTotal(), result.getRecords(), dto.getPageNum(), dto.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(UserAddDTO dto) {
        // 检查用户名唯一
        long count = count(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.USER_ALREADY_EXISTS);
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setUserType(dto.getUserType());
        user.setStudentNo(dto.getStudentNo());
        user.setStaffNo(dto.getStaffNo());
        user.setMajor(dto.getMajor());
        user.setStatus(1);
        save(user);
        UserClassRelationEnum relationType = relationTypeOf(user.getUserType());
        if (relationType == null) {
            return;
        }
        List<Integer> classIds = relationType == UserClassRelationEnum.STUDENT
                ? (dto.getClassId() == null ? Collections.emptyList() : List.of(dto.getClassId()))
                : (dto.getClassIds() == null ? Collections.emptyList() : dto.getClassIds());
        userClassService.replaceClassIds(user.getId(), relationType, classIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateDTO dto) {
        User user = getById(dto.getId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        BeanUtil.copyProperties(dto, user, "id");
        updateById(user);
        UserClassRelationEnum relationType = relationTypeOf(user.getUserType());
        if (relationType == null) {
            return;
        }
        if (relationType == UserClassRelationEnum.STUDENT) {
            List<Integer> classIds = dto.getClassId() == null
                    ? Collections.emptyList()
                    : List.of(dto.getClassId());
            userClassService.replaceClassIds(user.getId(), relationType, classIds);
        } else if (dto.getClassIds() != null) {
            userClassService.replaceClassIds(user.getId(), relationType, dto.getClassIds());
        }
    }

    @Override
    public void toggleStatus(Integer id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        user.setStatus(user.getStatus() == 1 ? 2 : 1);
        updateById(user);
    }

    @Override
    public void resetPassword(Integer id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode("123456"));
        updateById(user);
    }

    private void fillClassIds(List<UserManageVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Integer> studentIds = records.stream()
                .filter(vo -> Integer.valueOf(1).equals(vo.getUserType()))
                .map(UserManageVO::getId)
                .toList();
        List<Integer> teacherIds = records.stream()
                .filter(vo -> Integer.valueOf(2).equals(vo.getUserType()))
                .map(UserManageVO::getId)
                .toList();
        Map<Integer, List<Integer>> studentClasses =
                userClassService.listClassIdsByUserIds(studentIds, UserClassRelationEnum.STUDENT);
        Map<Integer, List<Integer>> teacherClasses =
                userClassService.listClassIdsByUserIds(teacherIds, UserClassRelationEnum.TEACHER);
        for (UserManageVO vo : records) {
            List<Integer> classIds = Integer.valueOf(1).equals(vo.getUserType())
                    ? studentClasses.get(vo.getId())
                    : teacherClasses.get(vo.getId());
            vo.setClassIds(classIds == null ? Collections.emptyList() : classIds);
        }
    }

    private UserClassRelationEnum relationTypeOf(Integer userType) {
        if (Integer.valueOf(1).equals(userType)) {
            return UserClassRelationEnum.STUDENT;
        }
        if (Integer.valueOf(2).equals(userType)) {
            return UserClassRelationEnum.TEACHER;
        }
        return null;
    }
}
