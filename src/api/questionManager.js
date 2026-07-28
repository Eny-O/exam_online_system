import request from "@/utils/request";

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
