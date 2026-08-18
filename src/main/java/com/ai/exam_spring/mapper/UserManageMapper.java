package com.ai.exam_spring.mapper;

import com.ai.exam_spring.pojo.vo.user.UserManageVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserManageMapper {

    /**
     * 分页查询人员管理列表
     */
    Page<UserManageVO> pageUserManage(
            Page<UserManageVO> page,
            @Param("realName") String realName,
            @Param("userId") String userId,
            @Param("userType") Integer userType,
            @Param("status") Integer status,
            @Param("classId") Integer classId
    );
}
