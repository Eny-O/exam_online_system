import axios from "axios";
import request from "@/utils/request";

/**
 * 分页查询人员列表
 */
export function getUserManagePage(params) {
  return request.get("/user/manage/page", { params });
}

/**
 * 新增用户
 */
export function addUser(data) {
  return request.post("/user/manage/add", data);
}

/**
 * 修改用户
 */
export function updateUser(data) {
  return request.put("/user/manage/update", data);
}

/**
 * 切换用户状态
 */
export function toggleUserStatus(id) {
  return request.put("/user/manage/toggleStatus", null, { params: { id } });
}

/**
 * 重置密码
 */
export function resetUserPassword(id) {
  return request.put("/user/manage/resetPassword", null, { params: { id } });
}

/**
 * 获取班级列表（复用已有接口）
 */
export function getClassList() {
  return request.get("/sysClass/list");
}

/**
 * 批量导入学生（上传 Excel）
 */
export function importUsers(file) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/user/import", formData);
}

/**
 * 下载学生导入模板（二进制流，走裸 axios）
 */
export function downloadUserTemplate() {
  return axios({
    url: "/api/user/template",
    method: "GET",
    responseType: "blob",
    headers: { token: localStorage.getItem("token") },
  });
}
