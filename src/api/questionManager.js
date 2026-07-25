import request from "@/utils/request";

// 分页条件查询题目列表
export function getQuestionPage(params) {
  return request.get("/questionManager/page", { params });
}
