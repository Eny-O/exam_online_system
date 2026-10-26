package com.ai.exam_spring.modules.user.service.user.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.constant.JwtConstant;
import com.ai.exam_spring.common.constant.RedisConstant;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.common.pojo.result.PageResult;
import com.ai.exam_spring.modules.user.pojo.dto.StudentQueryDTO;
import com.ai.exam_spring.modules.user.pojo.dos.SysClass;
import com.ai.exam_spring.modules.user.pojo.dos.User;
import com.ai.exam_spring.modules.user.pojo.enums.UserStatusEnum;
import com.ai.exam_spring.modules.user.pojo.vo.PermissionVO;
import com.ai.exam_spring.modules.user.pojo.vo.UStudentVO;
import com.ai.exam_spring.modules.user.mapper.UserMapper;
import com.ai.exam_spring.security.LoginUser;
import com.ai.exam_spring.modules.user.pojo.dto.UserLoginDTO;
import com.ai.exam_spring.modules.user.pojo.vo.UserInfoVO;
import com.ai.exam_spring.modules.user.service.user.ISysClassService;
import com.ai.exam_spring.modules.user.service.user.UserService;
import com.ai.exam_spring.utils.JwtUtil;
import com.ai.exam_spring.utils.RedisUtil;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ISysClassService sysClassService;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 用户登录
     * <p>
     * 通过 Spring Security 认证后，将 LoginUser 存入 Redis 供后续 JWT 过滤器使用，
     * 返回包含 Token 的 HashMap。
     *
     * @param userLoginDTO 登录请求参数
     * @return 包含 token 键值对的 Map
     */
    @Override
    public HashMap<String,String> login(UserLoginDTO userLoginDTO) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userLoginDTO.getUsername(), userLoginDTO.getPassword());
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        if (Objects.isNull(authenticate)) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        log.info("认证成功");
        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
        // status: 1-启用, 2-禁用
        if (loginUser.getUser().getStatus() == UserStatusEnum.DISABLED.getCode()) {
            throw new BusinessException(ErrorCodeEnum.ACCOUNT_DISABLED);
        }
        String userId = loginUser.getUser().getId().toString();
        String token = JwtUtil.generateToken(userId, null);
        // 将 LoginUser 缓存到 Redis，JWT 过滤器通过此缓存还原用户身份
        redisUtil.set(RedisConstant.LOGIN_USER_PREFIX_KEY + userId, loginUser);
        HashMap<String, String> result = new HashMap<>();
        result.put(JwtConstant.TOKEN_HEADER, token);
       return result;
    }

    /**
     * 用户登出，清除 Redis 中的登录缓存
     */
    @Override
    public void logout() {
        redisUtil.delete(RedisConstant.LOGIN_USER_PREFIX_KEY+ SecurityUtil.getCurrentUserId());
    }

    /**
     * 获取用户信息
     * 从securityContextHolder中获取loginUser中的user和permission,并映射成userInfoVO
     * @return 用户信息
     */
    @Override
    public UserInfoVO getInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(loginUser.getUser().getId());
        userInfoVO.setUsername(loginUser.getUser().getUsername());
        userInfoVO.setRealName(loginUser.getUser().getRealName());
        userInfoVO.setClassId(loginUser.getUser().getClassId());
        userInfoVO.setStatus(loginUser.getUser().getStatus());
        List<PermissionVO> permissionVOList = loginUser.getPermissions().stream().map(permission -> {
            PermissionVO permissionVO = new PermissionVO();
            BeanUtil.copyProperties(permission, permissionVO);
            return permissionVO;
        }).toList();
        userInfoVO.setPermissions(permissionVOList);
        return userInfoVO;
    }

    /**
     * 获取班级成员
     * @param classId 班级id
     * @return 班级成员
     */
    @Override
    public List<UStudentVO> getClassMember(Integer classId) {
        if (classId == null) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        SysClass sysClass = sysClassService.getById(classId);
        if (sysClass == null) throw new BusinessException(ErrorCodeEnum.CLASS_NOT_FOUND);
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getClassId, classId));
        if (users == null || users.isEmpty()) return  null;
        return users.stream().map(user -> {
            UStudentVO uStudentVO = new UStudentVO();
            BeanUtil.copyProperties(user, uStudentVO);
            uStudentVO.setGrade(sysClass.getGrade());
            uStudentVO.setClassName(sysClass.getClassName());
            return uStudentVO;
        }).toList();
    }

    /**
     * 分页查询学生
     * @param studentQueryDTO 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<UStudentVO> pageStudent(StudentQueryDTO studentQueryDTO) {
        if (studentQueryDTO == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Page<UStudentVO> page = Page.of(studentQueryDTO.getPageNum(), studentQueryDTO.getPageSize());
        Wrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.isNotBlank(studentQueryDTO.getRealName()),User::getRealName, studentQueryDTO.getRealName())
                .like(StringUtils.isNotBlank(studentQueryDTO.getStudentNo()),User::getStudentNo, studentQueryDTO.getStudentNo())
                .eq(studentQueryDTO.getClassId() != null, User::getClassId, studentQueryDTO.getClassId());
        Page<UStudentVO> userPage = userMapper.pageStudent(page, wrapper);
        return PageResult.of(userPage.getTotal(), userPage.getRecords(), studentQueryDTO.getPageNum(), studentQueryDTO.getPageSize());
    }
}
