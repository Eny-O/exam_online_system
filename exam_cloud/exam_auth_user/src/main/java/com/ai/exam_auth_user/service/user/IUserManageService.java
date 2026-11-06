package com.ai.exam_auth_user.service.user;

import com.ai.exam_common.common.pojo.result.BatchInsertResult;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_auth_user.pojo.dto.UserAddDTO;
import org.springframework.web.multipart.MultipartFile;
import com.ai.exam_auth_user.pojo.dto.RegisterDTO;
import com.ai.exam_auth_user.pojo.dto.UserQueryDTO;
import com.ai.exam_auth_user.pojo.dto.UserUpdateDTO;
import com.ai.exam_auth_user.pojo.vo.UserManageVO;

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
     * 批量导入学生（仅学生）：返回成功数与失败行详情
     */
    BatchInsertResult importStudents(MultipartFile file);

    /**
     * 学生自助注册（固定角色为学生）
     */
    void register(RegisterDTO registerDTO);

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
