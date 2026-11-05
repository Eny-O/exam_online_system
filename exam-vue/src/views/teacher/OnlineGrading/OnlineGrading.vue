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
            'nav-full': q.score !== null && q.score === q.fullScore,
            'nav-partial':
              q.score !== null && q.score > 0 && q.score < q.fullScore,
            'nav-zero': q.score !== null && q.score === 0,
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
          @click="currentQuestionIndex = index"
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

            <!-- AI 智能评分（仅主观题，单题试验功能） -->
            <div class="ai-grade-section" v-if="isEssay(q.type)">
              <el-button
                size="small"
                type="primary"
                plain
                :loading="aiLoadingKey === aiKey(q.questionId)"
                @click="handleAiGrade(index)"
              >
                AI 评分
              </el-button>
              <el-alert
                v-if="
                  aiResults[aiKey(q.questionId)] &&
                  aiResults[aiKey(q.questionId)].status !== 'UNGRADABLE' &&
                  aiResults[aiKey(q.questionId)].status !== 'ERROR'
                "
                class="ai-result"
                :type="
                  aiResults[aiKey(q.questionId)].status === 'NEED_HUMAN_REVIEW'
                    ? 'warning'
                    : 'success'
                "
                :closable="false"
                show-icon
              >
                <template #title>
                  <span>
                    AI 初评 {{ aiResults[aiKey(q.questionId)].preliminaryScore }} /
                    再评 {{ aiResults[aiKey(q.questionId)].finalScore }} 分，分歧
                    {{
                      Math.round(
                        (aiResults[aiKey(q.questionId)].divergence || 0) * 100
                      )
                    }}%
                  </span>
                </template>
                <div
                  v-if="
                    aiResults[aiKey(q.questionId)].reviewResult &&
                    aiResults[aiKey(q.questionId)].reviewResult.reviewComment
                  "
                  class="ai-comment"
                >
                  {{ aiResults[aiKey(q.questionId)].reviewResult.reviewComment }}
                </div>
              </el-alert>
              <el-alert
                v-if="aiResults[aiKey(q.questionId)]?.status === 'ERROR'"
                class="ai-result"
                type="error"
                :closable="false"
                show-icon
                :title="aiResults[aiKey(q.questionId)]?.errorMessage || 'AI 服务调用失败'"
              />
            </div>
          </el-card>
        </div>
      </el-scrollbar>

      <!-- 底部操作栏（固定） -->
      <div class="action-bar">
        <div class="total-score-display">
          总分：<strong>{{ totalScore }}</strong> / {{ paperTotalScore }}
        </div>
        <div class="action-buttons">
          <el-button @click="handleGoBack">返回</el-button>
          <el-button
            v-if="hasUngradedObjective && currentStudentId"
            type="warning"
            @click="handleAutoGradeObjective"
          >
            自动批阅客观题
          </el-button>
          <el-button @click="handleSaveCurrent">保存当前学生</el-button>
          <el-button type="primary" @click="handleSaveAndNext">
            保存并下一位
          </el-button>
          <el-button type="success" @click="handleGradingComplete">
            批阅完成
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getGradePaper,
  getGradeStudentList,
  getStudentAnswer,
  updateGradeStudentScore,
  updateExamStatus,
  autoGradeObjectiveQuestions,
} from "@/api/examTask";
import { aiSubjectiveGrade } from "@/api/aiGrading";

const route = useRoute();
const router = useRouter();
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

// ========== AI 智能批阅 ==========
// 正在 AI 评分的题目 key（「学生:题目」），用于单题按钮 loading 态
const aiLoadingKey = ref(null);
// 各题 AI 批阅结果缓存（按「学生:题目」隔离），用于展示初评/再评/分歧/复核意见
const aiResults = reactive({});
// AI 批阅结果按「学生:题目」隔离，避免切换学生后结果串到其他学生/题目
const aiKey = (questionId) => `${currentStudentId.value}:${questionId}`;
// 按学生缓存未保存的答题编辑，避免切换学生时 AI 填分等未保存改动丢失
const studentAnswersCache = reactive({});

// ========== 已批改记录 { [studentId]: true } ==========
const gradedStudents = reactive({});

// ========== 合并展示数据：题目结构 + 学生作答 ==========
const displayQuestions = computed(() => {
  return questionList.value.map((q) => {
    const answer = studentAnswers.value.find((a) => a.questionId === q.id);
    return {
      answerId: answer?.id ?? null,
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

// 当前学生是否有未批阅的客观题
const hasUngradedObjective = computed(() => {
  return displayQuestions.value.some(
    (q) => isObjective(q.type) && q.isCorrect === null
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
  // 自动加载第一个学生的答题情况
  if (studentList.value.length > 0) {
    await handleSelectStudent(studentList.value[0]);
  }
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
  } finally {
    loadingAnswers.value = false;
  }
};

const loadStudentList = async () => {
  loadingStudents.value = true;
  try {
    const res = await getGradeStudentList(examId);
    const list = Array.isArray(res?.data) ? res.data : [];
    studentList.value = list;
    list.forEach((s) => {
      if (s?.studentId != null && s.status === "已批阅") {
        gradedStudents[s.studentId] = true;
      }
    });
  } catch (e) {
    console.error("加载学生列表失败:", e);
  } finally {
    loadingStudents.value = false;
  }
};

const loadStudentAnswers = async (studentId) => {
  // 优先使用已缓存的未保存编辑（含 AI 填分），避免切换学生时丢失
  if (studentAnswersCache[studentId]) {
    studentAnswers.value = studentAnswersCache[studentId];
    return;
  }
  loadingAnswers.value = true;
  try {
    const res = await getStudentAnswer(studentId, examId);
    const data = res.data || [];
    studentAnswers.value = data;
    studentAnswersCache[studentId] = data;
  } catch (e) {
    console.error("加载学生答题失败:", e);
    studentAnswers.value = [];
  } finally {
    loadingAnswers.value = false;
  }
};

// ========== 交互方法 ==========
const handleSelectStudent = async (student) => {
  if (currentStudentId.value === student.studentId) return;
  // 暂存当前学生的未保存编辑（含 AI 填分），切回时可恢复
  if (currentStudentId.value && studentAnswers.value.length) {
    studentAnswersCache[currentStudentId.value] = studentAnswers.value;
  }
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
    // 主观题根据分数更新 isCorrect
    if (q.type === "ESSAY") {
      if (q.score === null || q.score === undefined) {
        answer.isCorrect = null;
      } else if (q.score === q.fullScore) {
        answer.isCorrect = 1;
      } else if (q.score === 0) {
        answer.isCorrect = 0;
      } else {
        answer.isCorrect = 2;
      }
    }
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

// 根据分数计算 isCorrect 状态
const computeIsCorrect = (score, fullScore, type) => {
  // 客观题保持手动设置的对错标记
  if (["SINGLE", "MULTIPLE", "JUDGE"].includes(type)) {
    return null; // 客观题使用 displayQuestions 中的 isCorrect
  }
  // 主观题根据分数判断
  if (score === null || score === undefined) return null; // 未批阅
  if (score === fullScore) return 1; // 满分
  if (score === 0) return 0; // 零分
  return 2; // 半对
};

// 构建保存请求的数据
const buildSavePayload = () => {
  return displayQuestions.value
    .filter((q) => q.answerId !== null)
    .map((q) => {
      // 客观题使用手动标记的 isCorrect，主观题根据分数计算
      let isCorrect;
      if (["SINGLE", "MULTIPLE", "JUDGE"].includes(q.type)) {
        isCorrect = q.isCorrect;
      } else {
        isCorrect = computeIsCorrect(q.score, q.fullScore, q.type);
      }
      return {
        id: q.answerId,
        examId: examId,
        studentId: currentStudentId.value,
        questionId: q.questionId,
        type: q.type ? q.type.toLowerCase() : "",
        isCorrect: isCorrect,
        score: q.score,
      };
    });
};

// 保存批阅结果到后端
const saveGrading = async () => {
  if (!currentStudentId.value) {
    ElMessage.warning("请先选择一位学生");
    return false;
  }
  const payload = buildSavePayload();
  if (payload.length === 0) {
    ElMessage.warning("没有可保存的批阅数据");
    return false;
  }
  try {
    await updateGradeStudentScore(payload);
    markCurrentStudentGraded();
    // 保存成功后同步缓存，确保切回时仍是已保存状态
    studentAnswersCache[currentStudentId.value] = studentAnswers.value;
    return true;
  } catch (e) {
    console.error("保存批阅失败:", e);
    return false;
  }
};

const handleSaveCurrent = async () => {
  const success = await saveGrading();
  if (success) {
    ElMessage.success("保存成功");
  }
};

const handleSaveAndNext = async () => {
  const success = await saveGrading();
  if (!success) return;
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

// 自动批阅客观题
const handleAutoGradeObjective = async () => {
  if (!currentStudentId.value) {
    ElMessage.warning("请先选择一位学生");
    return;
  }
  try {
    await autoGradeObjectiveQuestions(examId, currentStudentId.value);
    ElMessage.success("客观题自动批阅完成");
    // 重新加载该学生的答题数据以刷新页面
    await loadStudentAnswers(currentStudentId.value);
  } catch (e) {
    console.error("自动批阅客观题失败:", e);
  }
};

// AI 智能双评单题（人机协同：回填建议分 + 展示双评意见，教师确认/微调后保存）
// 结果按「学生:题目」隔离（aiKey），保证回显到正确位置，不串到其他学生
const handleAiGrade = async (index) => {
  const q = displayQuestions.value[index];
  if (!currentStudentId.value) {
    ElMessage.warning("请先选择一位学生");
    return;
  }
  if (!q.questionId) {
    ElMessage.warning("题目信息缺失，无法调用 AI 批阅");
    return;
  }
  try {
    await ElMessageBox.confirm(
      "该功能为试验功能，AI 评分结果可能不稳定，仅作参考，请人工复核后再保存。是否继续？",
      "试验功能提示",
      {
        confirmButtonText: "仍要使用",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
  } catch {
    return;
  }
  const key = aiKey(q.questionId);
  aiLoadingKey.value = key;
  try {
    const res = await aiSubjectiveGrade({
      examId,
      studentId: currentStudentId.value,
      questionId: q.questionId,
    });
    const outcome = res.data;
    aiResults[key] = outcome;
    if (outcome.status === "UNGRADABLE") {
      ElMessage.warning("AI 暂无法评判该题（题目或作答信息不足）");
      return;
    }
    // 回填 AI 最终分（限制在题目满分内，防止越界）
    const finalScore = Math.min(outcome.finalScore ?? 0, q.fullScore);
    q.score = finalScore;
    onScoreChange(index); // 复用同步逻辑：写回 studentAnswers 并设置 isCorrect
    if (outcome.status === "NEED_HUMAN_REVIEW") {
      ElMessage.warning("AI 两评分歧较大，已填入建议分，请人工复核后再保存");
    } else {
      ElMessage.success("AI 评分完成，请确认或微调后点「保存当前学生」");
    }
  } catch (e) {
    console.error("AI 评分失败:", e);
    const errMsg = e?.response?.data?.message || e?.message || "AI 服务调用失败";
    aiResults[key] = { status: "ERROR", errorMessage: errMsg };
  } finally {
    aiLoadingKey.value = null;
  }
};

const handleGoBack = () => {
  router.push("/teacher/examTask/myexam");
};

const handleGradingComplete = () => {
  ElMessageBox.confirm("确认所有学生的批阅都已完成并提交吗？", "批阅完成确认", {
    confirmButtonText: "确认完成",
    cancelButtonText: "继续批阅",
    type: "warning",
  })
    .then(async () => {
      try {
        await updateExamStatus(examId, 5);
        ElMessage.success("批阅完成！");
        router.push("/teacher/examTask/myexam");
      } catch (error) {
        console.error("批阅完成状态更新失败:", error);
      }
    })
    .catch(() => {
      // 用户取消，继续批阅
    });
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

/* 满分 - 绿色 */
.question-number.nav-full {
  background-color: #f0f9eb;
  color: #67c23a;
  border-color: #67c23a;
}

.question-number.active.nav-full {
  background-color: #67c23a;
  color: #ffffff;
  border-color: #67c23a;
}

/* 半对 - 橙色 */
.question-number.nav-partial {
  background-color: #fdf6ec;
  color: #e6a23c;
  border-color: #e6a23c;
}

.question-number.active.nav-partial {
  background-color: #e6a23c;
  color: #ffffff;
  border-color: #e6a23c;
}

/* 零分 - 红色 */
.question-number.nav-zero {
  background-color: #fef0f0;
  color: #f56c6c;
  border-color: #f56c6c;
}

.question-number.active.nav-zero {
  background-color: #f56c6c;
  color: #ffffff;
  border-color: #f56c6c;
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

/* 底部操作栏（固定） */
.action-bar {
  padding: 16px 20px;
  background-color: #ffffff;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
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
.ai-grade-section {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ai-result {
  margin-top: 4px;
}
.ai-comment {
  margin-top: 4px;
  line-height: 1.5;
  color: #606266;
  white-space: pre-wrap;
}
</style>
