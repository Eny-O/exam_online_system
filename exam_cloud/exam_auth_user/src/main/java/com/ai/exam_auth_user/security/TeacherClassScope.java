package com.ai.exam_auth_user.security;

import com.ai.exam_auth_user.pojo.enums.UserClassRelationEnum;
import com.ai.exam_auth_user.service.user.ISysClassService;
import com.ai.exam_auth_user.service.user.IUserClassService;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 当前登录用户的任教班级范围
 */
@Component
public class TeacherClassScope {

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private IUserClassService userClassService;

    @Autowired
    private ISysClassService sysClassService;

    /**
     * 查询当前登录用户可见的启用班级ID
     *
     * @return null 表示不限制；教师返回任教班级ID列表
     */
    public List<Integer> scopedClassIds() {
        String userId = securityUtil.getCurrentUserId();
        Integer userType = securityUtil.getCurrentUserType();
        if (userId == null || userType == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        }
        if (!Integer.valueOf(2).equals(userType)) {
            return null;
        }
        List<Integer> classIds = userClassService.listClassIds(Integer.valueOf(userId), UserClassRelationEnum.TEACHER);
        List<Integer> activeIds = sysClassService.filterActiveClassIds(classIds);
        return activeIds == null ? List.of() : activeIds;
    }

    /**
     * 校验班级是否在当前登录用户可见范围内
     */
    public void assertClassVisible(Integer classId) {
        if (classId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        List<Integer> classIds = scopedClassIds();
        if (classIds != null && !classIds.contains(classId)) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }
    }
}
