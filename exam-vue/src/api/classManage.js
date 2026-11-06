import request from "@/utils/request";

/** 分页查询班级 */
export function getClassManagePage(params) {
  return request.get("/sysClass/page", { params });
}

/** 新增班级 */
export function addClass(data) {
  return request.post("/sysClass/add", data);
}

/** 编辑班级 */
export function updateClass(data) {
  return request.put("/sysClass/update", data);
}

/** 删除班级 */
export function deleteClass(id) {
  return request.delete("/sysClass/delete", { params: { id } });
}
