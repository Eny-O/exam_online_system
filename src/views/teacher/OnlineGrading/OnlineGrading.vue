<template>
  <div class="grading-container">
    <!-- 左侧学生列表 -->
    <div class="left-panel">
      <div class="panel-header">
        <div class="header-title">待批阅</div>
        <div class="header-count">{{ pendingCount }}</div>
      </div>

      <el-scrollbar class="student-list" v-loading="loadingStudents">
        <div
          v-for="student in studentList"
          :key="student.studentId"
          class="student-item"
          :class="{ active: currentStudentId === student.studentId }"
          @click="handleSelectStudent(student)"
        >
          <div class="student-info">
            <div class="student-name">{{ student.studentName }}</div>
            <div class="student-no">{{ student.studentNo }}</div>
          </div>
          <el-badge
            v-if="isStudentGraded(student.studentId)"
            value="已批"
            type="success"
          />
        </div>
        <div
          v-if="!loadingStudents && studentList.length === 0"
          class="empty-tip"
        >
          暂无待批阅学生
        </div>
      </el-scrollbar>
    </div>

    <!-- 右侧阅卷区域 -->
    <div class="right-panel" v-loading="loadingAnswers">
      <!-- 题号导航 -->
      <div class="question-nav" v-if="displayQuestions.length > 0">
        <div
          v-for="(q, index) in displayQuestions"
          :key="q.questionId"
          class="question-number"
          :class="{
            active: currentQuestionIndex === index,
            graded: q.isGraded,
          }"
          @click="handleSelectQuestion(index)"
        >
          {{ index + 1 }}
        </div>
      </div>

      <!-- 空状态 -->
      <div
        v-if="
          !loadingAnswers && displayQuestions.length === 0 && !currentStudentId
        "
        class="empty-state"
      >
        <p>请从左侧选择一位学生开始批阅</p>
      </div>
      <div
        v-if="
          !loadingAnswers && displayQuestions.length === 0 && currentStudentId
        "
        class="empty-state"
      >
        <p>暂无答题数据</p>
      </div>

      <!-- 题目内容区域 -->
      <el-scrollbar
        v-show="displayQuestions.length > 0"
        class="question-scroll"
      >
        <div
          v-for="(q, index) in displayQuestions"
          :key="q.questionId"
          :ref="
            (el) => {
              if (el) questionRefs[index] = el;
            }
          "
          class="question-block"
          :class="{ 'highlight-block': currentQuestionIndex === index }"
        >
          <el-card shadow="hover">
            <!-- 题目标头 -->
            <div class="question-block-header">
              <div class="question-left">
                <span class="q-index">第 {{ index + 1 }} 题</span>
                <el-tag
                  size="small"
                  :type="getTypeTag(q.type)"
                  class="type-tag"
                >
                  {{ getTypeLabel(q.type) }}
                </el-tag>
                <span class="q-score">满分 {{ q.fullScore }} 分</span>
              </div>
              <div class="question-right" v-if="isObjective(q.type)">
                <span class="result-label">判定：</span>
                <el-tag
                  v-if="q.isCorrect === 1"
                  type="success"
                  effect="dark"
                  class="result-tag"
                >
                  正确
                </el-tag>
                <el-tag
                  v-else-if="q.isCorrect === 0"
                  type="danger"
                  effect="dark"
                  class="result-tag"
                >
                  错误
                </el-tag>
                <el-tag v-else type="info" effect="dark" class="result-tag">
                  未判定
                </el-tag>
                <el-button
                  size="small"
                  text
                  type="primary"
                  @click="toggleCorrect(index)"
                >
                  修改
                </el-button>
              </div>
            </div>

            <!-- 题干 -->
            <div class="question-content">{{ q.content }}</div>

            <!-- 选项（客观题） -->
            <div v-if="q.options && q.options.length > 0" class="options-area">
              <div
                v-for="opt in q.options"
                :key="opt.id"
                class="option-item"
                :class="{
                  'correct-option': opt.label === q.correctAnswer,
                }"
              >
                <span class="option-label">{{ opt.label }}.</span>
                <span class="option-content">{{ opt.content }}</span>
                <span
                  v-if="opt.label === q.correctAnswer"
                  class="answer-hint correct-hint"
                >
                  正确答案
                </span>
              </div>
            </div>

            <!-- 正确答案（判断题） -->
            <div
              v-if="isJudge(q.type) && q.correctAnswer"
              class="correct-answer-row"
            >
              <span class="answer-label">正确答案：</span>
              <el-tag type="success" size="small">
                {{ q.correctAnswer === "A" ? "正确" : "错误" }}
              </el-tag>
            </div>

            <!-- 学生作答 -->
            <div class="student-answer-section">
              <div class="answer-label-row">
                <span class="answer-label">学生作答：</span>
                <el-tag
                  v-if="isEssay(q.type)"
                  :type="q.isGraded ? 'success' : 'warning'"
                  size="small"
                >
                  {{ q.isGraded ? "已批阅" : "待批阅" }}
                </el-tag>
              </div>
              <div
                class="answer-content"
                :class="{ 'no-answer': !q.studentAnswer }"
              >
                {{ formatStudentAnswer(q) }}
              </div>
            </div>

            <!-- 得分 -->
            <div class="score-section">
              <span class="score-label">得分：</span>
              <el-input-number
                v-model="q.score"
                :min="0"
                :max="q.fullScore"
                :precision="0"
                size="small"
                style="width: 130px"
                @change="onScoreChange(index)"
              />
              <span class="score-max">/ {{ q.fullScore }} 分</span>
            </div>
          </el-card>
        </div>

        <!-- 底部操作栏 -->
        <div class="action-bar">
          <div class="total-score-display">
            总分：<strong>{{ totalScore }}</strong> / {{ paperTotalScore }}
          </div>
          <div class="action-buttons">
            <el-button @click="handleSaveCurrent">保存当前学生</el-button>
            <el-button type="primary" @click="handleSaveAndNext">
              保存并下一位
            </el-button>
          </div>
        </div>
      </el-scrollbar>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import {
  getGradePaper,
  getGradeStudentList,
  getStudentAnswer,
} from "@/api/examTask";

const route = useRoute();
const examId = Number(route.query.examId);

// ========== 加载状态 ==========
const loadingStudents = ref(false);
const loadingAnswers = ref(false);

// ========== 试卷数据（只加载一次） ==========
const paperInfo = ref({});
const questionList = ref([]);

// ========== 学生列表 ==========
const studentList = ref([]);
const currentStudentId = ref(null);

// ========== 学生答题数据（按学生动态加载） ==========
const studentAnswers = ref([]);
const currentQuestionIndex = ref(0);
const questionRefs = reactive({});

// ========== 已批改记录 { [studentId]: true } ==========
const gradedStudents = reactive({});

// ========== 合并展示数据：题目结构 + 学生作答 ==========
const displayQuestions = computed(() => {
  return questionList.value.map((q) => {
    const answer = studentAnswers.value.find((a) => a.questionId === q.id);
    return {
      questionId: q.id,
      content: q.content,
      type: q.type,
      fullScore: q.score,
      correctAnswer: q.correctAnswer,
      options: q.options || [],
      studentAnswer: answer?.studentAnswer || "",
      isCorrect: answer?.isCorrect ?? null,
      score: answer?.score ?? null,
      isGraded: answer?.isCorrect !== null && answer?.isCorrect !== undefined,
    };
  });
});

// 待批阅人数
const pendingCount = computed(() => {
  return studentList.value.filter((s) => !gradedStudents[s.studentId]).length;
});

// 试卷总分
const paperTotalScore = computed(() => {
  return questionList.value.reduce((sum, q) => sum + (q.score || 0), 0);
});

// 当前学生已得总分
const totalScore = computed(() => {
  return displayQuestions.value.reduce(
    (sum, q) => sum + (typeof q.score === "number" ? q.score : 0),
    0
  );
});

// ========== 工具方法 ==========
const isObjective = (type) => ["SINGLE", "MULTIPLE", "JUDGE"].includes(type);
const isEssay = (type) => type === "ESSAY";
const isJudge = (type) => type === "JUDGE";

const getTypeLabel = (type) => {
  const map = {
    SINGLE: "单选题",
    MULTIPLE: "多选题",
    JUDGE: "判断题",
    ESSAY: "简答题",
  };
  return map[type] || type;
};

const getTypeTag = (type) => {
  const map = {
    SINGLE: "primary",
    MULTIPLE: "warning",
    JUDGE: "info",
    ESSAY: "danger",
  };
  return map[type] || "";
};

const formatStudentAnswer = (q) => {
  if (!q.studentAnswer) return "暂无作答";
  if (isEssay(q.type)) return q.studentAnswer;
  if (isJudge(q.type)) {
    return q.studentAnswer === "A" ? "正确" : "错误";
  }
  return q.studentAnswer;
};

const isStudentGraded = (studentId) => !!gradedStudents[studentId];

// ========== 初始化加载 ==========
onMounted(async () => {
  if (!examId) {
    ElMessage.error("缺少考试ID");
    return;
  }
  await Promise.all([loadPaper(), loadStudentList()]);
});

const loadPaper = async () => {
  loadingAnswers.value = true;
  try {
    const res = await getGradePaper(examId);
    const data = res.data;
    paperInfo.value = data;
    questionList.value = (data.gradePaperQuestionVOS || []).map((q) => ({
      id: q.id,
      sortOrder: q.sortOrder,
      score: q.score,
      content: q.content,
      type: q.type,
      correctAnswer: q.correctAnswer,
      options: q.options || [],
    }));
  } catch (e) {
    console.error("加载批阅试卷失败:", e);
    ElMessage.error("加载试卷内容失败");
  } finally {
    loadingAnswers.value = false;
  }
};

const loadStudentList = async () => {
  loadingStudents.value = true;
  try {
    const res = await getGradeStudentList(examId);
    studentList.value = res.data || [];
  } catch (e) {
    console.error("加载学生列表失败:", e);
    ElMessage.error("加载学生列表失败");
  } finally {
    loadingStudents.value = false;
  }
};

const loadStudentAnswers = async (studentId) => {
  loadingAnswers.value = true;
  try {
    const res = await getStudentAnswer(studentId, examId);
    studentAnswers.value = res.data || [];
  } catch (e) {
    console.error("加载学生答题失败:", e);
    ElMessage.error("加载学生答题情况失败");
    studentAnswers.value = [];
  } finally {
    loadingAnswers.value = false;
  }
};

// ========== 交互方法 ==========
const handleSelectStudent = async (student) => {
  if (currentStudentId.value === student.studentId) return;
  currentStudentId.value = student.studentId;
  currentQuestionIndex.value = 0;
  await loadStudentAnswers(student.studentId);
};

const handleSelectQuestion = (index) => {
  currentQuestionIndex.value = index;
  const el = questionRefs[index];
  if (el) {
    el.scrollIntoView({ behavior: "smooth", block: "center" });
  }
};

const toggleCorrect = (index) => {
  const q = displayQuestions.value[index];
  if (q.isCorrect === 1) {
    q.isCorrect = 0;
  } else if (q.isCorrect === 0) {
    q.isCorrect = 1;
  } else {
    q.isCorrect = 1;
  }
  // 同步到 studentAnswers 以便后续保存
  const answer = studentAnswers.value.find(
    (a) => a.questionId === q.questionId
  );
  if (answer) {
    answer.isCorrect = q.isCorrect;
  }
};

const onScoreChange = (index) => {
  const q = displayQuestions.value[index];
  const answer = studentAnswers.value.find(
    (a) => a.questionId === q.questionId
  );
  if (answer) {
    answer.score = q.score;
  }
};

const checkAllGraded = () => {
  return displayQuestions.value.every(
    (q) => q.isCorrect !== null && q.isCorrect !== undefined
  );
};

const markCurrentStudentGraded = () => {
  if (currentStudentId.value && checkAllGraded()) {
    gradedStudents[currentStudentId.value] = true;
  }
};

const handleSaveCurrent = () => {
  if (!currentStudentId.value) {
    ElMessage.warning("请先选择一位学生");
    return;
  }
  markCurrentStudentGraded();
  ElMessage.success("保存成功");
};

const handleSaveAndNext = () => {
  if (!currentStudentId.value) {
    ElMessage.warning("请先选择一位学生");
    return;
  }
  markCurrentStudentGraded();
  ElMessage.success("保存成功");

  const currentIdx = studentList.value.findIndex(
    (s) => s.studentId === currentStudentId.value
  );
  // 优先找下一个未批阅的学生
  let nextStudent = null;
  for (let i = 1; i < studentList.value.length; i++) {
    const idx = (currentIdx + i) % studentList.value.length;
    const s = studentList.value[idx];
    if (!gradedStudents[s.studentId]) {
      nextStudent = s;
      break;
    }
  }

  if (nextStudent) {
    handleSelectStudent(nextStudent);
  } else {
    ElMessage.success("所有学生已批阅完成！");
  }
};
</script>

<style scoped>
.grading-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background-color: #f5f7fa;
}

/* ===== 左侧面板 ===== */
.left-panel {
  width: 280px;
  min-width: 280px;
  background-color: #ffffff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-count {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.student-list {
  flex: 1;
  overflow: hidden;
}

.student-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid #f0f0f0;
}

.student-item:hover {
  background-color: #f5f7fa;
}

.student-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.student-info {
  flex: 1;
}

.student-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 3px;
}

.student-no {
  font-size: 13px;
  color: #909399;
}

.empty-tip {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
  font-size: 14px;
}

/* ===== 右侧面板 ===== */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  overflow: hidden;
}

/* 题号导航 */
.question-nav {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  flex-wrap: wrap;
}

.question-number {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background-color: #f5f7fa;
  color: #606266;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.question-number:hover {
  background-color: #ecf5ff;
  color: #409eff;
}

.question-number.active {
  background-color: #409eff;
  color: #ffffff;
  border-color: #409eff;
}

.question-number.graded {
  background-color: #f0f9eb;
  color: #67c23a;
  border-color: #67c23a;
}

.question-number.active.graded {
  background-color: #67c23a;
  color: #ffffff;
  border-color: #67c23a;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 16px;
}

/* 题目内容滚动区 */
.question-scroll {
  flex: 1;
  overflow: hidden;
}

/* 题目块 */
.question-block {
  padding: 16px 20px 0;
}

.question-block:last-child {
  padding-bottom: 0;
}

.highlight-block :deep(.el-card) {
  border-color: #409eff;
}

.question-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.question-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.q-index {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.type-tag {
  margin-left: 2px;
}

.q-score {
  font-size: 14px;
  color: #909399;
}

.question-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.result-label {
  font-size: 14px;
  color: #606266;
}

.result-tag {
  cursor: default;
}

/* 题干 */
.question-content {
  font-size: 15px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 12px;
  padding: 10px 12px;
  background-color: #fafafa;
  border-radius: 6px;
}

/* 选项 */
.options-area {
  margin-bottom: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 4px;
  margin-bottom: 4px;
  font-size: 14px;
}

.option-item.correct-option {
  background-color: #f0f9eb;
}

.option-label {
  font-weight: 600;
  color: #606266;
  min-width: 20px;
}

.option-content {
  color: #303133;
}

.answer-hint {
  margin-left: auto;
  font-size: 12px;
}

.correct-hint {
  color: #67c23a;
  font-weight: 500;
}

/* 判断题正确答案 */
.correct-answer-row {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 学生作答 */
.student-answer-section {
  margin-bottom: 12px;
}

.answer-label-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.answer-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.answer-content {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 6px;
  border-left: 3px solid #409eff;
  white-space: pre-wrap;
  word-break: break-word;
}

.answer-content.no-answer {
  color: #c0c4cc;
  font-style: italic;
  border-left-color: #dcdfe6;
}

/* 得分 */
.score-section {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e4e7ed;
}

.score-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.score-max {
  font-size: 14px;
  color: #909399;
}

/* 底部操作栏 */
.action-bar {
  padding: 16px 20px;
  background-color: #ffffff;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-score-display {
  font-size: 16px;
  color: #303133;
}

.total-score-display strong {
  color: #409eff;
  font-size: 20px;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

/* 滚动条 */
:deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}
</style>
