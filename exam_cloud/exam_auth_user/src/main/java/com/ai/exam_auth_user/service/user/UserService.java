package com.ai.exam_auth_user.service.user;

import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_auth_user.pojo.dto.StudentQueryDTO;
import com.ai.exam_auth_user.pojo.dto.UserLoginDTO;
import com.ai.exam_auth_user.pojo.dos.User;
import com.ai.exam_auth_user.pojo.vo.UserInfoVO;
import com.ai.exam_auth_user.pojo.vo.UStudentVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

/**
 * 用户服务接口
 */
@Service
public interface UserService extends IService<User> {
    /**
     * 用户登录，返回包含 Token 的 Map
     */
    HashMap<String,String> login(UserLoginDTO userLoginDTO);

    /**
     * 用户登出，清除 Redis 登录缓存
     */
    void logout();

    /**
     * 获取当前登录用户信息
     */
    UserInfoVO getInfo();

    /**
     * 根据班级id获取班级成员
     * @param classId
     * @return
     */
    List<UStudentVO> getClassMember(Integer classId);

    /**
     * 分页条件查询学生
     * @param studentQueryDTO
     * @return
     */
    PageResult<UStudentVO> pageStudent(StudentQueryDTO studentQueryDTO);
}
