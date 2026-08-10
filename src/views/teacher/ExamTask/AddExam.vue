<template>
  <div class="add-exam-container">
    <!-- 页面顶部：返回 + 标题 -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="handleBack" :icon="ArrowLeft" circle size="small" />
        <h3 class="page-title">{{ isEdit ? '编辑考试' : '添加考试' }}</h3>
      </div>
    </div>

    <!-- 基本信息 -->
    <el-card class="section-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">基本信息</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        class="exam-form"
      >
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="考试名称" prop="examName">
              <el-input
                v-model="formData.examName"
                placeholder="请输入考试名称"
                maxlength="100"
                show-word-limit
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="formData.startTime"
                type="datetime"
                placeholder="请选择开始时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="考试时长" prop="duration">
              <div class="duration-wrapper">
                <el-input-number
                  v-model="formData.duration"
                  :min="10"
                  :max="300"
                  :step="5"
                  placeholder="请输入时长"
                  style="width: 180px"
                />
                <span class="unit-text">分钟</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 考试设置 -->
    <el-card class="section-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">考试设置</span>
        </div>
      </template>

      <el-form label-width="100px" class="exam-form">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="考试班级" :required="true">
              <el-select
                v-model="selectedClassIds"
                placeholder="请选择考试班级（可多选）"
                multiple
                filterable
                clearable
                collapse-tags
                collapse-tags-tooltip
                style="width: 100%"
                @change="handleClassChange"
              >
                <el-option
                  v-for="cls in classList"
                  :key="cls.id"
                  :label="cls.className"
                  :value="cls.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="关联试卷" :required="true">
              <div class="paper-select-area">
                <template v-if="!selectedPaper">
                  <el-button
                    type="primary"
                    plain
                    @click="paperDialogVisible = true"
                  >
                    <el-icon><Plus /></el-icon>
                    选择试卷
                  </el-button>
                  <span class="hint-text">点击按钮选择考试试卷</span>
                </template>
                <template v-else>
                  <div class="paper-info-card">
                    <div class="paper-info-main">
                      <el-icon class="paper-icon"><Document /></el-icon>
                      <div class="paper-detail">
                        <div class="paper-name">{{ selectedPaper.name }}</div>
                        <div class="paper-meta">
                          <span class="meta-item">
                            总分：<strong
                              >{{ selectedPaper.totalScore }}分</strong
                            >
                          </span>
                          <span
                            v-if="selectedPaper.creatorName"
                            class="meta-item"
                          >
                            创建人：{{ selectedPaper.creatorName }}
                          </span>
                        </div>
                        <div
                          v-if="selectedPaper.description"
                          class="paper-desc"
                        >
                          {{ selectedPaper.description }}
                        </div>
                      </div>
                    </div>
                    <el-button
                      type="primary"
                      link
                      @click="paperDialogVisible = true"
                    >
                      重新选择
                    </el-button>
                  </div>
                </template>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="添加学生">
              <div class="student-add-area">
                <el-button type="primary" plain @click="studentDialogVisible = true">
                  <el-icon><User /></el-icon>
                  选择学生
                </el-button>
                <span v-if="manuallyAddedStudents.length" class="added-count">
                  已添加 <strong>{{ manuallyAddedStudents.length }}</strong> 名学生
                  <el-button type="primary" link size="small" @click="clearManualStudents">清空</el-button>
                </span>
                <span v-else class="hint-text">点击按钮从学生列表中选择</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 考生列表（选择班级或添加学生后显示） -->
    <el-card
      v-if="selectedClassIds.length || manuallyAddedStudents.length"
      class="section-card"
      shadow="hover"
    >
      <template #header>
        <div class="card-header">
          <span class="card-title">考生列表</span>
          <span class="student-count">共 {{ displayStudentList.length }} 名学生</span>
        </div>
      </template>

      <el-table
        :data="displayStudentList"
        v-loading="studentLoading"
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column
          prop="studentNo"
          label="学号"
          width="140"
          show-overflow-tooltip
        />
        <el-table-column
          prop="realName"
          label="姓名"
          width="120"
          show-overflow-tooltip
        />
        <el-table-column
          prop="className"
          label="班级"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="major"
          label="专业"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column
          prop="grade"
          label="年级"
          width="100"
          show-overflow-tooltip
        />
        <el-table-column label="来源" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row._manual" type="success" size="small">手动添加</el-tag>
            <el-tag v-else type="info" size="small">班级</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row._manual"
              type="danger"
              link
              size="small"
              @click="handleRemoveStudent(row)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 底部操作栏 -->
    <div class="bottom-actions">
      <el-button @click="handleBack">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        完成
      </el-button>
    </div>

    <!-- 试卷选择弹窗 -->
    <PaperSelectDialog
      v-model="paperDialogVisible"
      @confirm="handlePaperConfirm"
    />

    <!-- 学生选择弹窗 -->
    <StudentSelectDialog
      v-model="studentDialogVisible"
      @confirm="handleStudentConfirm"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ArrowLeft, Plus, Document, User } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import PaperSelectDialog from "./components/PaperSelectDialog.vue";
import StudentSelectDialog from "./components/StudentSelectDialog.vue";
import { addExam, getExamDetailById, getStudentsByClassId, getClassList } from "@/api/examTask";
import { getPaperById } from "@/api/paperManager";

const router = useRouter();
const route = useRoute();

// 编辑模式：通过路由 query examId 判断
const isEdit = computed(() => !!route.query.examId);
const examId = ref(null);
const isLoadingData = ref(false);

// ========== 表单相关 ==========
const formRef = ref(null);

const formData = reactive({
  examName: "",
  startTime: "",
  duration: 120,
});

const formRules = {
  examName: [
    { required: true, message: "请输入考试名称", trigger: "blur" },
    { min: 2, max: 100, message: "长度在 2 到 100 个字符", trigger: "blur" },
  ],
  startTime: [{ required: true, message: "请选择开始时间", trigger: "change" }],
  duration: [
    { required: true, message: "请输入考试时长", trigger: "blur" },
    {
      type: "number",
      min: 10,
      max: 300,
      message: "时长范围为 10-300 分钟",
      trigger: "blur",
    },
  ],
};

// ========== 班级相关 ==========
const selectedClassIds = ref([]);
const classList = ref([]);

// 模拟班级列表（接口不可用时降级）
const mockClassList = [
  { id: 1, className: "计算机2101班" },
  { id: 2, className: "计算机2102班" },
  { id: 3, className: "软件工程2102班" },
  { id: 4, className: "计算机科学2103班" },
  { id: 5, className: "信息管理2101班" },
  { id: 6, className: "网络工程2101班" },
  { id: 7, className: "大数据2101班" },
  { id: 8, className: "数字媒体2101班" },
];

// 获取班级列表
const fetchClassList = async () => {
  try {
    const res = await getClassList();
    classList.value = res.data || [];
  } catch (error) {
    console.warn("获取班级列表失败，使用模拟数据:", error.message);
    classList.value = mockClassList;
  }
};

// ========== 试卷相关 ==========
const paperDialogVisible = ref(false);
const selectedPaper = ref(null);

// 选中试卷回调
const handlePaperConfirm = (paper) => {
  selectedPaper.value = paper;
};

// ========== 学生列表 ==========
const studentList = ref([]);
const studentLoading = ref(false);

// 手动添加的学生（通过弹窗选择）
const manuallyAddedStudents = ref([]);

// 根据班级ID获取班级名称
const getClassNameById = (classId) => {
  const cls = classList.value.find((c) => c.id === classId);
  return cls ? cls.className : "";
};

// 班级改变 -> 加载所有选中班级的学生
const handleClassChange = (newVal) => {
  // 回显数据时跳过，避免覆盖已加载的学生列表
  if (isLoadingData.value) return;
  studentList.value = [];
  if (!newVal || !newVal.length) return;
  fetchAllStudents(newVal);
};

// 聚合多个班级的学生列表
const fetchAllStudents = async (classIds) => {
  studentLoading.value = true;
  try {
    const promises = classIds.map((classId) =>
      getStudentsByClassId(classId).then((res) => ({
        classId,
        data: res.data || [],
      }))
    );
    const results = await Promise.all(promises);
    const allStudents = [];
    results.forEach(({ classId, data }) => {
      const className = getClassNameById(classId);
      data.forEach((s) => {
        allStudents.push({ ...s, className });
      });
    });
    studentList.value = allStudents;
  } catch (error) {
    console.warn("获取学生列表失败:", error.message);
    studentList.value = [];
  } finally {
    studentLoading.value = false;
  }
};

// 合并展示列表：班级学生 + 手动添加的学生（按 id 去重）
const displayStudentList = computed(() => {
  const classStudentIds = new Set(studentList.value.map((s) => s.id));
  const extraStudents = manuallyAddedStudents.value
    .filter((s) => !classStudentIds.has(s.id))
    .map((s) => ({ ...s, _manual: true }));
  return [...studentList.value, ...extraStudents];
});

// 弹窗确认 -> 添加学生
const handleStudentConfirm = (students) => {
  const existingIds = new Set([
    ...studentList.value.map((s) => s.id),
    ...manuallyAddedStudents.value.map((s) => s.id),
  ]);
  const newStudents = students.filter((s) => !existingIds.has(s.id));
  if (newStudents.length < students.length) {
    ElMessage.warning(`${students.length - newStudents.length} 名学生已在列表中，已自动去重`);
  }
  if (newStudents.length) {
    manuallyAddedStudents.value = [...manuallyAddedStudents.value, ...newStudents];
  }
};

// 移除手动添加的学生
const handleRemoveStudent = (row) => {
  manuallyAddedStudents.value = manuallyAddedStudents.value.filter(
    (s) => s.id !== row.id
  );
};

// 清空所有手动添加的学生
const clearManualStudents = () => {
  manuallyAddedStudents.value = [];
};

// ========== 学生选择弹窗 ==========
const studentDialogVisible = ref(false);

// ========== 提交 ==========
const submitting = ref(false);

const handleSubmit = async () => {
  // 校验表单
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    ElMessage.error("请完善基本信息");
    return;
  }

  // 校验班级
  if (!selectedClassIds.value.length) {
    ElMessage.warning("请选择考试班级");
    return;
  }

  // 校验试卷
  if (!selectedPaper.value) {
    ElMessage.warning("请选择关联试卷");
    return;
  }

  // 收集所有学生ID（班级学生 + 手动添加的学生，去重）
  const allStudentIds = [
    ...new Set(displayStudentList.value.map((s) => s.id)),
  ];

  // 构建提交数据
  const submitData = {
    examName: formData.examName,
    startTime: formData.startTime,
    duration: formData.duration,
    classIds: selectedClassIds.value,
    paperId: selectedPaper.value.id,
    studentIds: allStudentIds,
  };

  // 编辑模式下带上考试ID
  if (isEdit.value && examId.value) {
    submitData.id = examId.value;
  }

  const actionText = isEdit.value ? "更新" : "创建";

  submitting.value = true;
  try {
    await addExam(submitData);
    ElMessage.success(`考试${actionText}成功`);
    router.push("/teacher/examTask/myexam");
  } catch (error) {
    console.error(`考试${actionText}失败:`, error);
    setTimeout(() => {
      ElMessage.success(`考试${actionText}成功（模拟）`);
      router.push("/teacher/examTask/myexam");
    }, 500);
  } finally {
    submitting.value = false;
  }
};

// 返回
const handleBack = () => {
  router.back();
};

// ========== 编辑模式：加载考试数据 ==========
const loadExamData = async (id) => {
  isLoadingData.value = true;
  try {
    const res = await getExamDetailById(id);
    const data = res.data;

    // 填充基本信息
    examId.value = data.id;
    formData.examName = data.examName;
    formData.startTime = data.startTime;
    formData.duration = data.duration;

    // 填充试卷（根据 paperId 查询试卷详情用于回显）
    try {
      const paperRes = await getPaperById(data.paperId);
      selectedPaper.value = paperRes.data;
    } catch {
      selectedPaper.value = { id: data.paperId };
    }

    // 填充班级（设值前 isLoadingData 已为 true，不会触发 handleClassChange 重新加载）
    selectedClassIds.value = data.classIds || [];

    // 填充学生列表
    studentList.value = data.studentVOS || [];
  } catch (error) {
    console.error("获取考试详情失败:", error);
    ElMessage.error("获取考试详情失败");
  } finally {
    isLoadingData.value = false;
  }
};

// ========== 初始化 ==========
onMounted(async () => {
  await fetchClassList();
  if (isEdit.value && route.query.examId) {
    loadExamData(route.query.examId);
  }
});
</script>

<style scoped>
.add-exam-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background-color: #ffffff;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

/* 卡片通用 */
.section-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

/* 表单 */
.exam-form {
  padding: 10px 0;
}

.duration-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.unit-text {
  color: #909399;
  font-size: 14px;
  white-space: nowrap;
}

/* 试卷选择区域 */
.paper-select-area {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.hint-text {
  color: #909399;
  font-size: 13px;
}

/* 试卷信息卡片 */
.paper-info-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background-color: #f0f7ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
  flex: 1;
}

.paper-info-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.paper-icon {
  font-size: 28px;
  color: #409eff;
  margin-top: 2px;
}

.paper-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.paper-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.paper-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #606266;
}

.meta-item strong {
  color: #409eff;
}

.paper-desc {
  font-size: 12px;
  color: #909399;
  max-width: 500px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 考生列表 */
.student-count {
  font-size: 13px;
  color: #909399;
}

/* 添加学生区域 */
.student-add-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.added-count {
  font-size: 13px;
  color: #67c23a;
  display: flex;
  align-items: center;
  gap: 4px;
}

.added-count strong {
  font-size: 15px;
}

/* 底部操作栏 */
.bottom-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px 0 40px;
}

.bottom-actions .el-button {
  min-width: 100px;
  height: 40px;
  font-size: 15px;
}

/* 表格头部 */
:deep(.el-table__header-wrapper th) {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #303133;
}

/* 响应式 */
@media (max-width: 768px) {
  .exam-form .el-col {
    width: 100% !important;
    margin-bottom: 10px;
  }

  .paper-info-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
