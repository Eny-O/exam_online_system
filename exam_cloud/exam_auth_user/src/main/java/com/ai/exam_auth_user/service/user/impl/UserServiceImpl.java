package com.ai.exam_auth_user.service.user.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_common.common.constant.JwtConstant;
import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_common.common.security.LoginUserPrincipal;
import com.ai.exam_common.common.security.LoginUserSnapshot;
import com.ai.exam_common.common.security.SecurityUtil;
import com.ai.exam_auth_user.mapper.PermissionMapper;
import com.ai.exam_auth_user.pojo.dto.StudentQueryDTO;
import com.ai.exam_auth_user.pojo.dos.SysClass;
import com.ai.exam_auth_user.pojo.dos.User;
import com.ai.exam_auth_user.pojo.enums.UserStatusEnum;
import com.ai.exam_auth_user.pojo.vo.PermissionVO;
import com.ai.exam_auth_user.pojo.vo.UStudentVO;
import com.ai.exam_auth_user.mapper.UserMapper;
import com.ai.exam_auth_user.security.LoginUser;
import com.ai.exam_auth_user.pojo.dto.UserLoginDTO;
import com.ai.exam_auth_user.pojo.vo.UserInfoVO;
import com.ai.exam_auth_user.service.user.ISysClassService;
import com.ai.exam_auth_user.service.user.UserService;
import com.ai.exam_common.common.utils.JwtUtil;
import com.ai.exam_auth_user.utils.RedisUtil;
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

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private PermissionMapper permissionMapper;


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
        // 将登录态快照缓存到 Redis，JWT 过滤器通过此缓存还原用户身份。
        // 写入的是中立快照（common 的 LoginUserSnapshot）而非含 DO 的 LoginUser，
        // 这样业务服务读取时不需要依赖 auth-user 的实体类。
        LoginUserSnapshot snapshot = new LoginUserSnapshot();
        snapshot.setId(loginUser.getUser().getId());
        snapshot.setUsername(loginUser.getUser().getUsername());
        snapshot.setRealName(loginUser.getUser().getRealName());
        snapshot.setStudentNo(loginUser.getUser().getStudentNo());
        snapshot.setClassId(loginUser.getUser().getClassId());
        snapshot.setStatus(loginUser.getUser().getStatus());
        snapshot.setPermissions(loginUser.getPermissionCodes());

        redisUtil.set(RedisConstant.LOGIN_USER_PREFIX_KEY + userId, snapshot);

        HashMap<String, String> result = new HashMap<>();
        result.put(JwtConstant.TOKEN_HEADER, token);
       return result;
    }

    /**
     * 用户登出，清除 Redis 中的登录缓存
     */
    @Override
    public void logout() {
        redisUtil.delete(RedisConstant.LOGIN_USER_PREFIX_KEY+ securityUtil.getCurrentUserId());
    }


    /**
     * 获取用户信息
     * <p>
     * 登录态快照只承载身份与权限（id/用户名/姓名/学号/班级/状态/权限编码），
     * 用户档案（手机号/专业/用户类型等）与权限明细（permName/icon/path 等菜单信息）
     * 均按当前用户ID回查数据库，保证资料变更后立即生效。
     */
    @Override
    public UserInfoVO getInfo() {
        // 当前用户ID取自登录态（SecurityContext 中的 LoginUserPrincipal）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUserPrincipal principal = (LoginUserPrincipal) authentication.getPrincipal();
        Integer userId = principal.getId();

        // 档案信息查库：User 的字段与 UserInfoVO 同名，copyProperties 自动忽略 password
        // （UserInfoVO 无 password 字段），不会泄露密码
        User user = userMapper.selectById(userId);

        // 权限明细查库：快照只带权限编码，不带 permName/icon/path/component 等菜单信息
        List<PermissionVO> permissionVOList = permissionMapper.getPermissionsByUserId(userId)
                .stream()
                .map(permission -> {
                    PermissionVO permissionVO = new PermissionVO();
                    BeanUtil.copyProperties(permission, permissionVO);
                    return permissionVO;
                })
                .toList();

        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtil.copyProperties(user, userInfoVO);
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
