package com.ai.exam_spring.service.user;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.pojo.dto.user.UserAddDTO;
import com.ai.exam_spring.pojo.dto.user.UserQueryDTO;
import com.ai.exam_spring.pojo.dto.user.UserUpdateDTO;
import com.ai.exam_spring.pojo.vo.user.UserManageVO;

public interface IUserManageService {

    /**
     * 分页查询人员列表
     */
    PageResult<UserManageVO> pageUser(UserQueryDTO userQueryDTO);

    /**
     * 新增用户
     */
    void addUser(UserAddDTO userAddDTO);

    /**
     * 修改用户
     */
    void updateUser(UserUpdateDTO userUpdateDTO);

    /**
     * 切换用户状态（启用/禁用）
     */
    void toggleStatus(Integer id);

    /**
     * 重置密码为默认值 123456
     */
    void resetPassword(Integer id);
}
