package com.ai.exam_auth_user.controller;


import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.ai.exam_auth_user.pojo.dos.SysClass;
import com.ai.exam_auth_user.pojo.dto.ClassAddDTO;
import com.ai.exam_auth_user.pojo.dto.ClassQueryDTO;
import com.ai.exam_auth_user.pojo.dto.ClassUpdateDTO;
import com.ai.exam_auth_user.pojo.vo.SysClassVO;
import com.ai.exam_auth_user.security.TeacherClassScope;
import com.ai.exam_auth_user.service.user.ISysClassService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 班级表 前端控制器
 * </p>
 *
 * @author HP
 * @since 2026-08-03
 */
@Tag(name = "班级管理接口")
@RestController
@RequestMapping("/sysClass")
@Slf4j
public class SysClassController {

    @Autowired
    private ISysClassService sysClassService;

    @Autowired
    private TeacherClassScope teacherClassScope;

    @Operation(summary = "获取班级列表")
    @PreAuthorize("hasAuthority('tch:class:view')")
    @GetMapping("/list")
    public ResponseResult<List<SysClassVO>> list() {
        log.info("获取班级列表");
        List<Integer> scopedClassIds = teacherClassScope.scopedClassIds();
        List<SysClass> list;
        if (scopedClassIds == null) {
            list = sysClassService.list(new LambdaQueryWrapper<SysClass>()
                    .eq(SysClass::getDeleted, 0));
        } else if (scopedClassIds.isEmpty()) {
            list = Collections.emptyList();
        } else {
            list = sysClassService.listByIds(scopedClassIds);
        }
        List<SysClassVO> sysClassVOList = list.stream().map(sysClass -> {
            SysClassVO sysClassVO = new SysClassVO();
            BeanUtil.copyProperties(sysClass, sysClassVO);
            return sysClassVO;
        }).toList();
        return ResponseResult.success(sysClassVOList);
    }

    @Operation(summary = "分页查询班级")
    @PreAuthorize("hasAuthority('sys:class:view')")
    @GetMapping("/page")
    public ResponseResult<PageResult<SysClassVO>> page(ClassQueryDTO classQueryDTO) {
        log.info("分页查询班级: {}", classQueryDTO);
        return ResponseResult.success(sysClassService.pageClass(classQueryDTO));
    }

    @Operation(summary = "新增班级")
    @PreAuthorize("hasAuthority('sys:class:add')")
    @PostMapping("/add")
    public ResponseResult<Void> add(@Valid @RequestBody ClassAddDTO classAddDTO) {
        log.info("新增班级: {}", classAddDTO.getClassName());
        sysClassService.addClass(classAddDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "编辑班级")
    @PreAuthorize("hasAuthority('sys:class:edit')")
    @PutMapping("/update")
    public ResponseResult<Void> update(@Valid @RequestBody ClassUpdateDTO classUpdateDTO) {
        log.info("编辑班级: {}", classUpdateDTO.getId());
        sysClassService.updateClass(classUpdateDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "停用班级")
    @PreAuthorize("hasAuthority('sys:class:delete')")
    @DeleteMapping("/delete")
    public ResponseResult<Void> delete(@RequestParam Integer id) {
        log.info("停用班级: {}", id);
        sysClassService.deleteClass(id);
        return ResponseResult.success();
    }

    @Operation(summary = "恢复班级")
    @PreAuthorize("hasAuthority('sys:class:edit')")
    @PutMapping("/restore")
    public ResponseResult<Void> restore(@RequestParam Integer id) {
        log.info("恢复班级: {}", id);
        sysClassService.restoreClass(id);
        return ResponseResult.success();
    }


}
