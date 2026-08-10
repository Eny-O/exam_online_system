package com.ai.exam_spring.controller.teacher;


import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.pojo.entity.user.SysClass;
import com.ai.exam_spring.pojo.vo.user.SysClassVO;
import com.ai.exam_spring.service.ISysClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    @Operation(summary = "获取班级列表")
    @GetMapping("/list")
    public ResponseResult<List<SysClassVO>> list() {
        List<SysClass> list = sysClassService.list();
        List<SysClassVO> sysClassVOList = list.stream().map(sysClass -> {
            SysClassVO sysClassVO = new SysClassVO();
            BeanUtil.copyProperties(sysClass, sysClassVO);
            return sysClassVO;
        }).toList();
        return ResponseResult.success(sysClassVOList);
    }

}
