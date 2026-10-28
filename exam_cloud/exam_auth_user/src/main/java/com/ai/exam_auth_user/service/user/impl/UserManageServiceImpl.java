package com.ai.exam_auth_user.service.user.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_auth_user.pojo.dto.UserAddDTO;
import com.ai.exam_auth_user.pojo.dto.UserQueryDTO;
import com.ai.exam_auth_user.pojo.dto.UserUpdateDTO;
import com.ai.exam_auth_user.pojo.dos.User;
import com.ai.exam_auth_user.pojo.vo.UserManageVO;
import com.ai.exam_auth_user.mapper.UserManageMapper;
import com.ai.exam_auth_user.mapper.UserMapper;
import com.ai.exam_auth_user.service.user.IUserManageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserManageServiceImpl extends ServiceImpl<UserMapper, User> implements IUserManageService {

    @Autowired
    private UserManageMapper userManageMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserManageVO> pageUser(UserQueryDTO dto) {
        Page<UserManageVO> page = Page.of(dto.getPageNum(), dto.getPageSize());
        Page<UserManageVO> result = userManageMapper.pageUserManage(
                page, dto.getRealName(), dto.getUserId(),
                dto.getUserType(), dto.getStatus(), dto.getClassId()
        );
        return PageResult.of(result.getTotal(), result.getRecords(), dto.getPageNum(), dto.getPageSize());
    }

    @Override
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
        user.setClassId(dto.getClassId());
        user.setMajor(dto.getMajor());
        user.setStatus(1);
        save(user);
    }

    @Override
    public void updateUser(UserUpdateDTO dto) {
        User user = getById(dto.getId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        BeanUtil.copyProperties(dto, user, "id");
        updateById(user);
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
}
