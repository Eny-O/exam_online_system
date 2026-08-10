import request from "@/utils/request";

// 分页条件查询考试列表
export function getExamPage(params) {
  return request.get("/exam-task-management/page", { params });
}

// 新增考试
export function addExam(data) {
  return request.post("/exam-task-management/add", data);
}

// 根据班级ID获取学生列表
export function getStudentsByClassId(classId) {
  return request.get("/user/getClassMember", { params: { classId } });
}

// 获取班级列表（下拉框用）
export function getClassList() {
  return request.get("/sysClass/list");
}

// 获取所有考试状态（下拉框用）
export function getAllExamStatus() {
  return request.get("/exam-task-management/getAllExamStatus");
}

// 更改考试状态
export function updateExamStatus(id, status) {
  return request.put("/exam-task-management/updateExamStatus", null, {
    params: { id, status },
  });
}

// 学生考试分页查询
export function getStuExamPage(params) {
  return request.get("/stu/exam/page", { params });
}
