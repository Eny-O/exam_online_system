import request from "@/utils/request";
import axios from "axios";

// 分页条件查询题目列表
export function getQuestionPage(params) {
  return request.get("/questionManager/page", { params });
}

// 根据ID查询完整题目（含选项）
export function getQuestionCompleteById(id) {
  return request.get(`/questionManager/getQuestionCompleteById/${id}`);
}

// 新增题目
export function addQuestionComplete(data) {
  return request.post("/questionManager/addQuestionComplete", data);
}

// 更新题目
export function updateQuestionComplete(data) {
  return request.put("/questionManager/updateQuestionComplete", data);
}

// 删除题目（支持批量），后端 @RequestBody List<Integer>
export function deleteQuestions(ids) {
  return request.delete("/questionManager/deleteQuestionById", { data: ids });
}

// 批量导入题目（Excel文件上传）
export function importQuestions(file) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/fileManager/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

// 下载题目导入模板（二进制流，绕过 JSON 响应拦截器）
export function downloadQuestionTemplate() {
  return axios({
    url: "/api/fileManager/template",
    method: "GET",
    responseType: "blob",
    headers: { token: localStorage.getItem("token") },
  });
}
