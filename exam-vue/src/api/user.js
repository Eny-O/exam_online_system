import request from "@/utils/request";

// 登录
export function login(data) {
  return request.post("/user/login", data);
}

// 获取当前用户信息（含权限列表）
export function getUserInfo() {
  return request.get("/user/getInfo");
}

// 登出
export function logout() {
  return request.post("/user/logout");
}

// 学生自助注册
export function register(data) {
  return request.post("/user/register", data);
}
