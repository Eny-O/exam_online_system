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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
        entity.setGrade(normalizeText(dto.getGrade()));
        entity.setRemark(normalizeText(dto.getRemark()));
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
        entity.setGrade(normalizeText(dto.getGrade()));
        entity.setRemark(normalizeText(dto.getRemark()));
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
        if (isDisabled(entity)) {
            return;
        }
        long memberCount = userClassService.countByClassId(id);
        if (memberCount > 0) {
            log.info("停用班级仍有本库师生关联。classId={}, count={}", id, memberCount);
        }
        try {
            ResponseResult<Boolean> result = examClassFeignClient.hasClassReference(id);
            if (result != null && Boolean.TRUE.equals(result.getData())) {
                log.info("停用班级仍被历史考试引用。classId={}", id);
            }
        } catch (Exception e) {
            log.warn("班级考试引用校验失败。classId={}", id, e);
        }
        SysClass update = new SysClass();
        update.setId(id);
        update.setDeleted(id);
        updateById(update);
    }

    @Override
    public void restoreClass(Integer id) {
        if (id == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        SysClass entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.CLASS_NOT_FOUND);
        }
        if (!isDisabled(entity)) {
            return;
        }
        if (countByName(entity.getClassName(), id) > 0) {
            throw new BusinessException(ErrorCodeEnum.CLASS_NAME_DUPLICATE);
        }
        SysClass update = new SysClass();
        update.setId(id);
        update.setDeleted(0);
        try {
            updateById(update);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCodeEnum.CLASS_NAME_DUPLICATE);
        }
    }

    @Override
    public List<Integer> filterActiveClassIds(Collection<Integer> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> ids = classIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysClass> active = list(new LambdaQueryWrapper<SysClass>()
                .select(SysClass::getId)
                .in(SysClass::getId, ids)
                .eq(SysClass::getDeleted, 0));
        if (active == null || active.isEmpty()) {
            return Collections.emptyList();
        }
        return active.stream().map(SysClass::getId).toList();
    }

    private boolean isDisabled(SysClass entity) {
        Integer deleted = entity.getDeleted();
        return deleted != null && deleted != 0;
    }

    private long countByName(String className, Integer excludeId) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysClass>()
                .eq(SysClass::getClassName, className)
                .eq(SysClass::getDeleted, 0)
                .ne(excludeId != null, SysClass::getId, excludeId));
        return count == null ? 0L : count;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
