package com.ai.exam_auth_user.service.permission;

import com.ai.exam_auth_user.pojo.dto.PermissionAddDTO;
import com.ai.exam_auth_user.pojo.dto.PermissionUpdateDTO;
import com.ai.exam_auth_user.pojo.vo.PermissionTreeVO;

import java.util.List;

public interface IPermissionManageService {

    /**
     * 查询权限树形列表
     */
    List<PermissionTreeVO> getPermissionTree();

    /**
     * 新增权限
     */
    void addPermission(PermissionAddDTO dto);

    /**
     * 修改权限
     */
    void updatePermission(PermissionUpdateDTO dto);

    /**
     * 删除权限
     */
    void deletePermission(Integer id);
}
