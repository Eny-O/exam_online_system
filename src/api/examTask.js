import request from "@/utils/request";

// 分页条件查询考试列表
export function getExamPage(params) {
  return request.get("/exam-task-management/page", { params });
}

// 新增考试
export function addExam(data) {
  return request.post("/exam-task-management/add", data);
}

// 更新考试
export function updateExam(data) {
  return request.put("/exam-task-management/update", data);
}

// 根据id获取考试详情
export function getExamDetailById(id) {
  return request.get("/exam-task-management/getExamDetailById", {
    params: { id },
  });
}

// 根据班级ID获取学生列表
export function getStudentsByClassId(classId) {
  return request.get("/user/getClassMember", { params: { classId } });
}

// 分页条件查询学生
export function pageStudent(params) {
  return request.get("/user/pageStudent", { params });
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
// 学生查看考试详情
export function getExamInfo(id) {
  return request.get("/stu/exam/getExamInfo", { params: { id } });
}

// 学生获取试卷内容
export function getExamPaper(id) {
  return request.get("/stu/exam/getExamPaper", { params: { id } });
}

// 学生提交试卷（仅更新考试状态，答案已通过updateAnswer实时保存）
export function submitPaper(examId) {
  return request.post("/stu/exam/submitPaper", null, { params: { examId } });
}

// 初始化学生考试（校验考试和学生状态）
export function initStuExam(examId) {
  return request.get("/stu/exam/initStuExam", { params: { examId } });
}

// 学生修改答案（实时保存到Redis，提交时批量写入数据库）
export function updateAnswer(data) {
  return request.put("/stu/exam/updateAnswer", [data]);
}

// 学生获取答题详情
export function getAnswerDetail(examId) {
  return request.get("/stu/exam/getAnswerDetail", { params: { examId } });
}

// 学生获取考试结果
export function getExamResult(examId) {
  return request.get("/stu/exam/getExamResult", { params: { examId } });
}

// 学生获取所有考试成绩
export function getAllExamResult() {
  return request.get("/stu/exam/getAllExamResult");
}

// 获取批阅试卷内容
export function getGradePaper(examId) {
  return request.get("/gradePaper/getGradePaper", { params: { examId } });
}

// 获取要批阅的学生列表
export function getGradeStudentList(examId) {
  return request.get("/gradePaper/getGradeStudentList", { params: { examId } });
}

// 获取学生答题情况
export function getStudentAnswer(studentId, examId) {
  return request.get("/gradePaper/getStudentAnswer", {
    params: { studentId, examId },
  });
}

// 更新批阅的学生得分
export function updateGradeStudentScore(data) {
  return request.put("/gradePaper/updateGradeStudentScore", data);
}

// 自动批阅客观题
export function autoGradeObjectiveQuestions(examId, studentId) {
  return request.put("/gradePaper/autoGradeObjectiveQuestions", null, {
    params: { examId, studentId },
  });
}
