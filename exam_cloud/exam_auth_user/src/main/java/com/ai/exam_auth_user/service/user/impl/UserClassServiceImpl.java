package com.ai.exam_auth_user.service.user.impl;

import com.ai.exam_auth_user.mapper.UserClassMapper;
import com.ai.exam_auth_user.pojo.dos.UserClass;
import com.ai.exam_auth_user.pojo.enums.UserClassRelationEnum;
import com.ai.exam_auth_user.service.user.IUserClassService;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户-班级关联服务实现
 */
@Service
public class UserClassServiceImpl implements IUserClassService {

    @Autowired
    private UserClassMapper userClassMapper;

    @Override
    public List<Integer> listClassIds(Integer userId, UserClassRelationEnum relationType) {
        if (userId == null || relationType == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        List<UserClass> relations = userClassMapper.selectList(new LambdaQueryWrapper<UserClass>()
                .eq(UserClass::getUserId, userId)
                .eq(UserClass::getRelationType, relationType.getCode()));
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }
        return relations.stream()
                .map(UserClass::getClassId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    public Map<Integer, List<Integer>> listClassIdsByUserIds(Collection<Integer> userIds,
                                                             UserClassRelationEnum relationType) {
        if (relationType == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Integer> distinctIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserClass> relations = userClassMapper.selectList(new LambdaQueryWrapper<UserClass>()
                .in(UserClass::getUserId, distinctIds)
                .eq(UserClass::getRelationType, relationType.getCode()));
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyMap();
        }
        return relations.stream()
                .filter(relation -> relation.getUserId() != null && relation.getClassId() != null)
                .collect(Collectors.groupingBy(UserClass::getUserId,
                        Collectors.mapping(UserClass::getClassId, Collectors.toList())));
    }

    @Override
    public List<Integer> listUserIdsByClassId(Integer classId, UserClassRelationEnum relationType) {
        if (classId == null || relationType == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        List<UserClass> relations = userClassMapper.selectList(new LambdaQueryWrapper<UserClass>()
                .eq(UserClass::getClassId, classId)
                .eq(UserClass::getRelationType, relationType.getCode()));
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }
        return relations.stream()
                .map(UserClass::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    public long countByClassId(Integer classId) {
        if (classId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Long count = userClassMapper.selectCount(new LambdaQueryWrapper<UserClass>()
                .eq(UserClass::getClassId, classId));
        return count == null ? 0L : count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceClassIds(Integer userId, UserClassRelationEnum relationType,
                                Collection<Integer> classIds) {
        if (userId == null || relationType == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        userClassMapper.delete(new LambdaQueryWrapper<UserClass>()
                .eq(UserClass::getUserId, userId)
                .eq(UserClass::getRelationType, relationType.getCode()));
        if (classIds == null || classIds.isEmpty()) {
            return;
        }
        List<Integer> distinctIds = classIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        try {
            for (Integer classId : distinctIds) {
                UserClass relation = new UserClass();
                relation.setUserId(userId);
                relation.setClassId(classId);
                relation.setRelationType(relationType.getCode());
                userClassMapper.insert(relation);
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCodeEnum.STUDENT_CLASS_CONFLICT);
        }
    }
}
