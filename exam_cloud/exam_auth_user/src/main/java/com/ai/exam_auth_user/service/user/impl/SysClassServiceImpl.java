package com.ai.exam_auth_user.service.user.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_api.feign.ExamClassFeignClient;
import com.ai.exam_auth_user.pojo.dos.SysClass;
import com.ai.exam_auth_user.mapper.SysClassMapper;
import com.ai.exam_auth_user.pojo.dto.ClassAddDTO;
import com.ai.exam_auth_user.pojo.dto.ClassQueryDTO;
import com.ai.exam_auth_user.pojo.dto.ClassUpdateDTO;
import com.ai.exam_auth_user.pojo.vo.SysClassVO;
import com.ai.exam_auth_user.service.user.ISysClassService;
import com.ai.exam_auth_user.service.user.IUserClassService;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
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
@Slf4j
@Service
public class SysClassServiceImpl extends ServiceImpl<SysClassMapper, SysClass> implements ISysClassService {

    @Autowired
    private IUserClassService userClassService;

    @Autowired
    private ExamClassFeignClient examClassFeignClient;

    @Override
    public PageResult<SysClassVO> pageClass(ClassQueryDTO dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Page<SysClass> page = Page.of(dto.getPageNum(), dto.getPageSize());
        Page<SysClass> result = page(page, new LambdaQueryWrapper<SysClass>()
                .like(StringUtils.isNotBlank(dto.getClassName()), SysClass::getClassName, dto.getClassName())
                .eq(StringUtils.isNotBlank(dto.getGrade()), SysClass::getGrade, dto.getGrade())
                .orderByDesc(SysClass::getId));
        List<SysClassVO> records = result.getRecords().stream().map(entity -> {
            SysClassVO vo = new SysClassVO();
            BeanUtil.copyProperties(entity, vo);
            return vo;
        }).toList();
        return PageResult.of(result.getTotal(), records, dto.getPageNum(), dto.getPageSize());
    }

    @Override
    public void addClass(ClassAddDTO dto) {
        String className = dto.getClassName().trim();
        if (countByName(className, null) > 0) {
            throw new BusinessException(ErrorCodeEnum.CLASS_NAME_DUPLICATE);
        }
        SysClass entity = new SysClass();
        entity.setClassName(className);
        entity.setGrade(normalizeGrade(dto.getGrade()));
        try {
            save(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCodeEnum.CLASS_NAME_DUPLICATE);
        }
    }

    @Override
    public void updateClass(ClassUpdateDTO dto) {
        SysClass entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.CLASS_NOT_FOUND);
        }
        String className = dto.getClassName().trim();
        if (countByName(className, dto.getId()) > 0) {
            throw new BusinessException(ErrorCodeEnum.CLASS_NAME_DUPLICATE);
        }
        entity.setClassName(className);
        entity.setGrade(normalizeGrade(dto.getGrade()));
        try {
            updateById(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCodeEnum.CLASS_NAME_DUPLICATE);
        }
    }

    @Override
    public void deleteClass(Integer id) {
        if (id == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        SysClass entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.CLASS_NOT_FOUND);
        }
        if (userClassService.countByClassId(id) > 0) {
            throw new BusinessException(ErrorCodeEnum.CLASS_HAS_MEMBERS);
        }
        ResponseResult<Boolean> result;
        try {
            result = examClassFeignClient.hasClassReference(id);
        } catch (Exception e) {
            log.error("班级考试引用校验失败。classId={}", id, e);
            throw new BusinessException(ErrorCodeEnum.CLASS_REF_CHECK_FAILED);
        }
        if (result == null || result.getData() == null) {
            throw new BusinessException(ErrorCodeEnum.CLASS_REF_CHECK_FAILED);
        }
        if (Boolean.TRUE.equals(result.getData())) {
            throw new BusinessException(ErrorCodeEnum.CLASS_REFERENCED_BY_EXAM);
        }
        try {
            removeById(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCodeEnum.CLASS_HAS_MEMBERS);
        }
    }

    private long countByName(String className, Integer excludeId) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysClass>()
                .eq(SysClass::getClassName, className)
                .ne(excludeId != null, SysClass::getId, excludeId));
        return count == null ? 0L : count;
    }

    private String normalizeGrade(String grade) {
        if (grade == null) {
            return null;
        }
        String trimmed = grade.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
