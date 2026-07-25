package com.ai.exam_spring.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.constant.JwtConstant;
import com.ai.exam_spring.common.constant.RedisConstant;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.domain.vo.PermissionVO;
import com.ai.exam_spring.security.LoginUser;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.domain.dto.UserLoginDTO;
import com.ai.exam_spring.domain.vo.UserInfoVO;
import com.ai.exam_spring.service.UserService;
import com.ai.exam_spring.utils.JwtUtil;
import com.ai.exam_spring.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 登录
     * @param userLoginDTO
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
        if (loginUser.getUser().getStatus() != 1) {
            throw new BusinessException(ErrorCodeEnum.USER_FORBIDDEN);
        }
        String userId = loginUser.getUser().getId().toString();
        String token = JwtUtil.generateToken(userId, null);
        redisUtil.set(RedisConstant.LOGIN_USER_PREFIX_KEY +userId,loginUser);
        HashMap<String, String> hm = new HashMap<>();
        hm.put(JwtConstant.TOKEN_HEADER,token);
       return hm;
    }

    /**
     * 登出
     */
    @Override
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            redisUtil.delete("login: " + loginUser.getUser().getId());
        }
    }

    @Override
    public UserInfoVO getInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setUsername(loginUser.getUser().getUsername());
        userInfoVO.setRealName(loginUser.getUser().getRealName());
        userInfoVO.setClassId(loginUser.getUser().getClassId());
        userInfoVO.setStatus(loginUser.getUser().getStatus());
        // 创建权限VO列表
        List<PermissionVO> permissionVOList;
        //  将权限列表转换成权限VO列表
        permissionVOList = loginUser.getPermissions().stream().map(permission -> {
            PermissionVO permissionVO = new PermissionVO();
            BeanUtil.copyProperties(permission, permissionVO);
            return permissionVO;
        }).toList();
        userInfoVO.setPermissions(permissionVOList);
        return userInfoVO;
    }
}
