import request from "@/utils/request";

// AI 智能双评主观题（exam_ai 模块，经网关 /api/ai/** 路由到 lb://exam-ai）
// @param {{ examId: number, studentId: number, questionId: number }} data
// @returns {Promise} 响应体 data 为 SubjectiveGradeOutcome：
//   { status, finalScore, preliminaryScore, divergence, initialResult, reviewResult, escalationReason }
export function aiSubjectiveGrade(data) {
  return request.post("/ai/subjective/grade", data, { timeout: 300000 });
}
