package com.ai.exam_spring.modules.user.service.user;

import com.ai.exam_spring.common.pojo.result.PageResult;
import com.ai.exam_spring.modules.user.pojo.dto.UserAddDTO;
import com.ai.exam_spring.modules.user.pojo.dto.UserQueryDTO;
import com.ai.exam_spring.modules.user.pojo.dto.UserUpdateDTO;
import com.ai.exam_spring.modules.user.pojo.vo.UserManageVO;

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
