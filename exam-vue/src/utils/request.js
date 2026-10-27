import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
  baseURL: "/api",
  timeout: 10000,
});

// 请求拦截器：自动携带 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers["token"] = token;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
request.interceptors.response.use(
  // HTTP 2xx 走这里：检查后端业务状态码
  (response) => {
    const res = response.data;
    if (res.code !== "200") {
      ElMessage.error(res.message || "请求失败");
      return Promise.reject(new Error(res.message || "请求失败"));
    }
    return res;
  },
  // HTTP 4xx/5xx 走这里：处理网络/服务器层面的异常
  (error) => {
    let msg = "请求失败";
    if (error.response) {
      const data = error.response.data;
      if (data && typeof data === "object" && data.message) {
        msg = data.message;
      }
      if (error.response.status === 401) {
        localStorage.removeItem("token");
        if (window.location.hash !== "#/login") {
          window.location.hash = "#/login";
        }
      }
    }
    ElMessage.error(msg);
    return Promise.reject(new Error(msg));
  }
);

export default request;
