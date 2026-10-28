package com.ai.exam_auth_user.controller;

import com.ai.exam_api.pojo.dto.ClassBriefDTO;
import com.ai.exam_api.pojo.dto.UserBriefDTO;
import com.ai.exam_auth_user.mapper.SysClassMapper;
import com.ai.exam_auth_user.mapper.UserMapper;
import com.ai.exam_auth_user.pojo.dos.SysClass;
import com.ai.exam_auth_user.pojo.dos.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 内部用户查询接口（供 exam-core 等服务调用）
 * <p>
 * 只暴露只读、语义化的批量查询，避免把 IService 的通用 CRUD 暴露为跨服务契约——
 * 后者没有跨进程语义（LambdaQueryWrapper 无法传输），也不该成为服务间契约。
 * <p>
 * 返回的用户摘要不含密码等敏感字段。
 */
@Tag(name = "内部用户接口")
@RestController
@RequestMapping("/inner")
public class InnerUserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SysClassMapper sysClassMapper;

    @Operation(summary = "按ID批量查询用户摘要")
    @GetMapping("/user/briefs")
    public List<UserBriefDTO> listUserBriefs(@RequestParam("userIds") List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().map(user -> {
            UserBriefDTO dto = new UserBriefDTO();
            dto.setId(user.getId());
            dto.setStudentNo(user.getStudentNo());
            dto.setRealName(user.getRealName());
            dto.setMajor(user.getMajor());
            dto.setClassId(user.getClassId());
            return dto;
        }).toList();
    }

    @Operation(summary = "按ID批量查询班级摘要")
    @GetMapping("/class/briefs")
    public List<ClassBriefDTO> listClassBriefs(@RequestParam("classIds") List<Integer> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysClass> classes = sysClassMapper.selectBatchIds(classIds);
        return classes.stream().map(sysClass -> {
            ClassBriefDTO dto = new ClassBriefDTO();
            dto.setId(sysClass.getId());
            dto.setClassName(sysClass.getClassName());
            dto.setGrade(sysClass.getGrade());
            return dto;
        }).toList();
    }
}
