import request from "@/utils/request";

// 分页条件查询试卷列表
export function getPaperPage(params) {
  return request.get("/paperManager/page", { params });
}

// 根据ID查询试卷详情（含题目、选项、答案）
export function getPaperById(id) {
  return request.get(`/paperManager/queryPaperById/${id}`);
}

// 新增试卷
export function addPaper(data) {
  return request.post("/paperManager/addPaper", data);
}

// 更新试卷
export function updatePaper(data) {
  return request.put("/paperManager/updatePaper", data);
}

// 删除试卷（支持批量）
export function deletePapers(ids) {
  return request.delete("/paperManager/deletePaperById", { data: ids });
}

// 保存试卷组卷信息（题目列表）
export function savePaperQuestions(paperId, data) {
  return request.post(`/paperManager/${paperId}/questions`, data);
}

// 获取试卷的题目列表（含选项、答案）
export function getPaperQuestions(paperId) {
  return request.get(`/paperManager/${paperId}/questions`);
}

// 更新试卷状态（0草稿 1待启用 2已启用）
export function updatePaperStatus(id, status) {
  return request.put(`/paperManager/updatePaperStatus`, { id, status });
}
