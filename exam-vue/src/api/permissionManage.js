import request from "@/utils/request";

/**
 * 查询权限树形列表
 */
export function getPermissionTree() {
  return request.get("/permission/manage/tree");
}

/**
 * 新增权限
 */
export function addPermission(data) {
  return request.post("/permission/manage/add", data);
}

/**
 * 修改权限
 */
export function updatePermission(data) {
  return request.put("/permission/manage/update", data);
}

/**
 * 删除权限
 */
export function deletePermission(id) {
  return request.delete("/permission/manage/delete", { params: { id } });
}
