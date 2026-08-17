<template>
  <div class="exam-taking-container">
    <!-- 顶部导航栏 -->
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-wrapper">
      <el-icon class="is-loading loading-icon"><Loading /></el-icon>
      <p>正在加载试卷...</p>
    </div>

    <!-- 主体内容 -->
    <template v-else>
      <header class="exam-header">
        <div class="header-content">
          <div class="header-left">
            <el-button text @click="handleLeave">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <h1 class="exam-title">{{ paper.name }}</h1>
            <span class="total-score">总分：{{ paper.totalScore }}</span>
          </div>
          <div class="timer-display">
            <el-icon class="timer-icon"><Clock /></el-icon>
            <span :class="{ 'time-warning': timeLeft < 300 }">{{
              formatTime(timeLeft)
            }}</span>
          </div>
        </div>
      </header>

      <!-- 主体内容 -->
      <main class="exam-main">
        <!-- 左侧题目区 -->
        <div class="question-area">
          <transition name="fade-slide" mode="out-in">
            <div :key="currentIndex" class="question-card">
              <div class="question-header">
                <span class="question-index">第 {{ currentIndex + 1 }} 题</span>
                <el-tag size="small" :type="typeTagColor(currentQuestion.type)">
                  {{ typeLabel(currentQuestion.type) }}
                </el-tag>
                <span class="question-score"
                  >{{ currentQuestion.score }} 分</span
                >
                <el-tag
                  v-if="isMarked(currentIndex)"
                  size="small"
                  type="warning"
                  effect="plain"
                >
                  已标记
                </el-tag>
              </div>

              <p class="question-content">{{ currentQuestion.content }}</p>

              <!-- 客观题：单选 / 多选 / 判断 -->
              <div v-if="currentQuestion.type !== 'essay'" class="options-list">
                <button
                  v-for="opt in currentQuestion.options"
                  :key="opt.label"
                  @click="handleSelect(opt.label)"
                  :class="['option-item', { selected: isSelected(opt.label) }]"
                >
                  <div class="option-left">
                    <span
                      :class="[
                        'option-circle',
                        { selected: isSelected(opt.label) },
                      ]"
                    >
                      {{ opt.label }}
                    </span>
                    <span class="option-text">{{ opt.content }}</span>
                  </div>
                  <el-icon v-if="isSelected(opt.label)" class="check-icon"
                    ><Check
                  /></el-icon>
                </button>
              </div>

              <!-- 主观题：简答 -->
              <div v-else class="essay-area">
                <el-input
                  v-model="essayAnswers[currentQuestion.id]"
                  type="textarea"
                  :rows="6"
                  placeholder="请在此输入答案"
                  @input="handleEssayInput"
                />
              </div>
            </div>
          </transition>

          <!-- 底部操作栏 -->
          <div class="question-footer">
            <el-button
              :type="isMarked(currentIndex) ? 'warning' : 'default'"
              plain
              @click="toggleMark"
            >
              <el-icon><Flag /></el-icon>
              {{ isMarked(currentIndex) ? "取消标记" : "标记题目" }}
            </el-button>

            <div class="nav-buttons">
              <el-button @click="prevQuestion" :disabled="currentIndex === 0">
                <el-icon><ArrowLeft /></el-icon>
                上一题
              </el-button>

              <el-button
                v-if="currentIndex < questions.length - 1"
                type="primary"
                @click="nextQuestion"
              >
                下一题
                <el-icon><ArrowRight /></el-icon>
              </el-button>
              <el-button v-else type="success" @click="submitExam">
                提交试卷
              </el-button>
            </div>
          </div>
        </div>

        <!-- 右侧答题卡 -->
        <aside class="answer-sheet">
          <div class="sheet-card">
            <h3 class="sheet-title">题目导航</h3>
            <div class="sheet-grid">
              <button
                v-for="(q, index) in questions"
                :key="q.id"
                @click="currentIndex = index"
                :class="[
                  'sheet-item',
                  {
                    active: currentIndex === index,
                    answered: hasAnswer(q.id),
                    marked: isMarked(index),
                  },
                ]"
              >
                {{ index + 1 }}
              </button>
            </div>

            <div class="sheet-legend">
              <div class="legend-item">
                <span class="legend-dot answered"></span>
                <span>已作答</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot marked"></span>
                <span>已标记</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot"></span>
                <span>未作答</span>
              </div>
            </div>
          </div>
        </aside>
      </main>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Clock,
  Flag,
  Check,
  ArrowLeft,
  ArrowRight,
  Loading,
} from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getExamInfo,
  getExamPaper,
  submitPaper,
  initStuExam,
  updateAnswer,
} from "@/api/examTask";

const route = useRoute();
const router = useRouter();

// ========== 题型映射 ==========
const TYPE_LABEL = {
  single: "单选",
  multiple: "多选",
  judge: "判断",
  essay: "简答",
};

const TYPE_TAG_COLOR = {
  single: "",
  multiple: "warning",
  judge: "success",
  essay: "danger",
};

const typeLabel = (type) => TYPE_LABEL[type] ?? "未知";
const typeTagColor = (type) => TYPE_TAG_COLOR[type] ?? "info";

// ========== 状态定义 ==========
const loading = ref(true);
const examId = ref(null);
const paperId = ref(null);
const paper = ref({ name: "", totalScore: 0, stuPaperQuestionVOS: [] });
const questions = ref([]); // 扁平化后的题目数组
const currentIndex = ref(0);
const answers = ref({}); // { questionId: ['A'] or ['A','B'] }
const essayAnswers = ref({}); // { questionId: '文本内容' }
const markedIndices = ref(new Set());
const timeLeft = ref(0);
const questionIdMap = ref({}); // { questionId: studentAnswer主键id }
const essayTimers = {}; // 主观题防抖定时器 { questionId: timer }
let timer = null;

// ========== 数据获取 ==========
const fetchData = async () => {
  loading.value = true;
  examId.value = route.params.id;
  try {
    // 先获取考试信息，拿到 paperId
    const infoRes = await getExamInfo(examId.value);
    paperId.value = infoRes.data.paperId;

    // 根据开始时间和时长动态计算剩余时间
    const startTime = new Date(infoRes.data.startTime).getTime();
    const durationSeconds = infoRes.data.duration * 60;
    const endTime = startTime + durationSeconds * 1000;
    const now = Date.now();
    const remaining = Math.floor((endTime - now) / 1000);
    // 未开始则显示完整时长，已过期则为0
    timeLeft.value =
      remaining > durationSeconds ? durationSeconds : Math.max(remaining, 0);

    // 用 paperId 获取试卷内容
    const paperRes = await getExamPaper(infoRes.data.paperId);

    // 保存试卷原始数据
    paper.value = paperRes.data;

    // 扁平化题目数据（后端返回type为大写，统一转小写匹配前端逻辑）
    questions.value = paperRes.data.stuPaperQuestionVOS.map((pq) => ({
      sortOrder: pq.sortOrder,
      score: pq.score,
      id: pq.stuQuestionCompleteVO.id,
      content: pq.stuQuestionCompleteVO.content,
      type: pq.stuQuestionCompleteVO.type?.toLowerCase(),
      options: pq.stuQuestionCompleteVO.options || [],
    }));
    console.log(
      "题目数据:",
      questions.value.map((q) => ({ id: q.id, type: q.type }))
    );

    // 初始化学生考试数据，获取 questionId -> studentAnswer主键id 的映射
    const initRes = await initStuExam(examId.value);
    questionIdMap.value = initRes.data || {};
    console.log("题目ID映射:", questionIdMap.value);
  } catch (error) {
    console.error("加载试卷失败:", error);
  } finally {
    loading.value = false;
  }
};

// ========== 计算属性 ==========
const currentQuestion = computed(() => questions.value[currentIndex.value]);

// ========== 方法 ==========
const formatTime = (seconds) => {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
};

const handleSelect = (optionLabel) => {
  const q = currentQuestion.value;
  const currentAns = answers.value[q.id] || [];

  if (q.type === "single" || q.type === "judge") {
    // 单选/判断：直接替换
    answers.value[q.id] = [optionLabel];
  } else {
    // 多选：切换状态
    if (currentAns.includes(optionLabel)) {
      answers.value[q.id] = currentAns.filter((l) => l !== optionLabel);
    } else {
      answers.value[q.id] = [...currentAns, optionLabel];
    }
  }

  // 实时保存到后端
  const answerId = questionIdMap.value[q.id];
  if (answerId) {
    const studentAnswer = answers.value[q.id].sort().join(",");
    updateAnswer({ id: answerId, studentAnswer }).catch((err) => {
      console.warn("保存答案失败:", err);
    });
  }
};

const isSelected = (optionLabel) => {
  const q = currentQuestion.value;
  const currentAns = answers.value[q.id] || [];
  return currentAns.includes(optionLabel);
};

const handleEssayInput = () => {
  // 主观题防抖保存：停止输入1秒后自动保存
  const q = currentQuestion.value;
  const answerId = questionIdMap.value[q.id];
  if (!answerId) return;

  // 清除该题目之前的定时器
  if (essayTimers[q.id]) {
    clearTimeout(essayTimers[q.id]);
  }
  // 设置新的防抖定时器
  essayTimers[q.id] = setTimeout(() => {
    const studentAnswer = essayAnswers.value[q.id] || "";
    updateAnswer({ id: answerId, studentAnswer }).catch((err) => {
      console.warn("保存主观题答案失败:", err);
    });
    delete essayTimers[q.id];
  }, 1000);
};

const hasAnswer = (questionId) => {
  // 客观题检查 answers，主观题检查 essayAnswers
  const optAns = answers.value[questionId];
  if (optAns && optAns.length > 0) return true;
  const essayAns = essayAnswers.value[questionId];
  return essayAns !== undefined && essayAns !== null && essayAns.trim() !== "";
};

const isMarked = (index) => {
  return markedIndices.value.has(index);
};

const toggleMark = () => {
  if (markedIndices.value.has(currentIndex.value)) {
    markedIndices.value.delete(currentIndex.value);
  } else {
    markedIndices.value.add(currentIndex.value);
  }
  markedIndices.value = new Set(markedIndices.value);
};

const prevQuestion = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--;
  }
};

const nextQuestion = () => {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++;
  }
};

const handleLeave = async () => {
  try {
    await ElMessageBox.confirm("是否确定要离开考试？", "离开确认", {
      confirmButtonText: "确定离开",
      cancelButtonText: "继续考试",
      type: "warning",
    });
    router.push("/exam/list");
  } catch {
    // 取消操作
  }
};

const submitExam = async (autoSubmit = false) => {
  if (!autoSubmit) {
    try {
      await ElMessageBox.confirm(
        `确定要提交试卷吗？剩余时间: ${formatTime(timeLeft.value)}`,
        "提交确认",
        {
          confirmButtonText: "确定提交",
          cancelButtonText: "再检查一下",
          type: "warning",
        }
      );
    } catch {
      return; // 取消操作
    }
  }

  // 构建提交数据：[{id, studentAnswer}]，id 为 studentAnswer 主键
  const payload = questions.value.map((q) => {
    const id = questionIdMap.value[q.id];
    let studentAnswer = "";
    if (q.type === "essay") {
      studentAnswer = essayAnswers.value[q.id] || "";
    } else {
      const selected = answers.value[q.id] || [];
      studentAnswer = selected.sort().join(",");
    }
    return { id, studentAnswer };
  });

  try {
    await submitPaper(payload);
    ElMessage.success("试卷提交成功！");
    router.push("/exam/list");
  } catch (error) {
    console.error("提交试卷失败:", error);
  }
};

// ========== 生命周期 ==========
onMounted(() => {
  fetchData();
  timer = setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--;
    } else {
      clearInterval(timer);
      ElMessage.warning("考试时间已到，自动提交试卷");
      submitExam(true);
    }
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
  // 清理所有主观题防抖定时器
  Object.values(essayTimers).forEach((t) => clearTimeout(t));
});
</script>

<style scoped>
.exam-taking-container {
  min-height: 100vh;
  background-color: #f9fafb;
  display: flex;
  flex-direction: column;
}

/* 加载状态 */
.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120px 0;
  color: #6b7280;
}

.loading-icon {
  font-size: 32px;
  color: #2563eb;
  margin-bottom: 16px;
}

/* 头部 */
.exam-header {
  background-color: white;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.total-score {
  font-size: 14px;
  color: #6b7280;
  white-space: nowrap;
}

.exam-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.timer-display {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #dc2626;
  font-family: monospace;
  font-size: 18px;
  font-weight: 500;
}

.timer-icon {
  width: 20px;
  height: 20px;
}

.time-warning {
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 主体布局 */
.exam-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
  display: flex;
  gap: 24px;
  width: 100%;
  box-sizing: border-box;
}

.question-area {
  flex: 1;
  min-width: 0; /* 防止flex子项溢出 */
}

.answer-sheet {
  width: 260px;
  flex-shrink: 0;
}

/* 题目卡片 */
.question-card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  padding: 24px;
  margin-bottom: 24px;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.question-index {
  font-size: 14px;
  color: #6b7280;
}

.question-score {
  font-size: 13px;
  color: #9ca3af;
}

.question-content {
  font-size: 18px;
  color: #1f2937;
  margin-bottom: 24px;
  line-height: 1.6;
}

/* 选项列表 */
.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  width: 100%;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  background-color: white;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.option-item:hover {
  border-color: #d1d5db;
  background-color: #f9fafb;
}

.option-item.selected {
  border-color: #3b82f6;
  background-color: #eff6ff;
}

.option-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.option-circle {
  width: 24px;
  height: 24px;
  border: 2px solid #d1d5db;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  transition: all 0.2s;
}

.option-item.selected .option-circle {
  border-color: #3b82f6;
  color: #3b82f6;
}

.option-text {
  color: #374151;
  font-size: 16px;
}

.check-icon {
  color: #3b82f6;
  width: 20px;
  height: 20px;
}

/* 简答区域 */
.essay-area {
  margin-top: 8px;
}

/* 底部操作 */
.question-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
}

.nav-buttons {
  display: flex;
  gap: 12px;
}

/* 答题卡 */
.sheet-card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  padding: 16px;
  position: sticky;
  top: 80px;
}

.sheet-title {
  font-size: 16px;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 16px 0;
}

.sheet-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.sheet-item {
  aspect-ratio: 1;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background-color: #f3f4f6;
  color: #4b5563;
}

.sheet-item:hover {
  background-color: #e5e7eb;
}

.sheet-item.active {
  background-color: #3b82f6;
  color: white;
}

.sheet-item.answered {
  background-color: #dcfce7;
  color: #166534;
}

.sheet-item.marked {
  background-color: #ffedd5;
  color: #9a3412;
}

/* 当既是已答又是标记时，优先显示已答颜色，或者可以加个角标，这里简化处理 */
.sheet-item.answered.marked {
  background-color: #dcfce7;
  color: #166534;
  position: relative;
}
.sheet-item.answered.marked::after {
  content: "";
  position: absolute;
  top: 2px;
  right: 2px;
  width: 6px;
  height: 6px;
  background-color: #f97316;
  border-radius: 50%;
}

.sheet-legend {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
  font-size: 12px;
  color: #6b7280;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 4px;
  background-color: #f3f4f6;
}

.legend-dot.answered {
  background-color: #dcfce7;
}

.legend-dot.marked {
  background-color: #ffedd5;
}

/* 动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .exam-main {
    flex-direction: column;
  }

  .answer-sheet {
    width: 100%;
  }

  .sheet-card {
    position: static;
  }

  .sheet-grid {
    grid-template-columns: repeat(10, 1fr);
  }
}
</style>
