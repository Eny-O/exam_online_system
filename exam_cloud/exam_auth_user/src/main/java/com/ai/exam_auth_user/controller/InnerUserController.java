package com.ai.exam_auth_user.controller;

import com.ai.exam_api.pojo.dto.ClassBriefDTO;
import com.ai.exam_api.pojo.dto.UserBriefDTO;
import com.ai.exam_auth_user.mapper.SysClassMapper;
import com.ai.exam_auth_user.mapper.UserMapper;
import com.ai.exam_auth_user.pojo.dos.SysClass;
import com.ai.exam_auth_user.pojo.dos.User;
import com.ai.exam_auth_user.pojo.enums.UserClassRelationEnum;
import com.ai.exam_auth_user.service.user.ISysClassService;
import com.ai.exam_auth_user.service.user.IUserClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
@Slf4j
public class InnerUserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SysClassMapper sysClassMapper;

    @Autowired
    private IUserClassService userClassService;

    @Autowired
    private ISysClassService sysClassService;

    @Operation(summary = "按ID批量查询用户摘要")
    @GetMapping("/user/briefs")
    public List<UserBriefDTO> listUserBriefs(@RequestParam("userIds") List<Integer> userIds) {
        log.info("批量查询用户摘要, count={}", userIds == null ? 0 : userIds.size());
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, List<Integer>> classMap =
                userClassService.listClassIdsByUserIds(userIds, UserClassRelationEnum.STUDENT);
        return users.stream().map(user -> {
            UserBriefDTO dto = new UserBriefDTO();
            dto.setId(user.getId());
            dto.setStudentNo(user.getStudentNo());
            dto.setRealName(user.getRealName());
            dto.setMajor(user.getMajor());
            List<Integer> classIds = classMap.get(user.getId());
            dto.setClassId(classIds == null || classIds.isEmpty() ? null : classIds.get(0));
            return dto;
        }).toList();
    }

    @Operation(summary = "查询教师任教班级ID")
    @GetMapping("/user/teacher-class-ids")
    public List<Integer> listTeacherClassIds(@RequestParam("userId") Integer userId) {
        log.info("查询教师任教班级ID: {}", userId);
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Integer> classIds = userClassService.listClassIds(userId, UserClassRelationEnum.TEACHER);
        List<Integer> activeIds = sysClassService.filterActiveClassIds(classIds);
        return activeIds == null ? Collections.emptyList() : activeIds;
    }

    @Operation(summary = "按ID批量查询班级摘要")
    @GetMapping("/class/briefs")
    public List<ClassBriefDTO> listClassBriefs(@RequestParam("classIds") List<Integer> classIds) {
        log.info("批量查询班级摘要, count={}", classIds == null ? 0 : classIds.size());
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
