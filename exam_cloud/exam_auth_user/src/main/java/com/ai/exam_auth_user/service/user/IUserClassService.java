package com.ai.exam_auth_user.service.user;

import com.ai.exam_auth_user.pojo.enums.UserClassRelationEnum;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户-班级关联服务
 */
public interface IUserClassService {

    /**
     * 查询用户关联的班级ID
     */
    List<Integer> listClassIds(Integer userId, UserClassRelationEnum relationType);

    /**
     * 批量查询用户关联的班级ID, 按用户ID分组
     */
    Map<Integer, List<Integer>> listClassIdsByUserIds(Collection<Integer> userIds,
                                                     UserClassRelationEnum relationType);

    /**
     * 查询班级下关联的用户ID
     */
    List<Integer> listUserIdsByClassId(Integer classId, UserClassRelationEnum relationType);

    /**
     * 统计班级的关联记录数
     */
    long countByClassId(Integer classId);

    /**
     * 替换用户关联的班级
     */
    void replaceClassIds(Integer userId, UserClassRelationEnum relationType,
                         Collection<Integer> classIds);
}
