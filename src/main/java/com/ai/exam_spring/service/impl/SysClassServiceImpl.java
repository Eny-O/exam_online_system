package com.ai.exam_spring.service.impl;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.domain.entity.SysClass;
import com.ai.exam_spring.domain.entity.User;
import com.ai.exam_spring.mapper.SysClassMapper;
import com.ai.exam_spring.service.ISysClassService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 班级表 服务实现类
 * </p>
 *
 * @author HP
 * @since 2026-08-03
 */
@Service
public class SysClassServiceImpl extends ServiceImpl<SysClassMapper, SysClass> implements ISysClassService {

}
