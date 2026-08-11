package com.ai.exam_spring.mapper;

import com.ai.exam_spring.pojo.entity.user.User;
import com.ai.exam_spring.pojo.vo.user.UStudentVO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

/**
 * 用户表 Mapper 接口
 */
public interface UserMapper extends BaseMapper<User> {
    Page<UStudentVO> pageStudent(Page<UStudentVO> page,@Param("ew") Wrapper<User> wrapper);
}
