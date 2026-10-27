import request from "@/utils/request";

/**
 * 查询所有角色
 */
export function listRoles() {
  return request.get("/role/auth/list");
}

/**
 * 查询完整权限树
 */
export function getPermissionTree() {
  return request.get("/role/auth/permissions");
}

/**
 * 查询角色已分配的权限ID
 */
export function getRolePermissionIds(roleId) {
  return request.get(`/role/auth/permissions/${roleId}`);
}

/**
 * 为角色分配权限
 */
export function assignPermissions(roleId, permissionIds) {
  return request.put("/role/auth/assign", { roleId, permissionIds });
}
