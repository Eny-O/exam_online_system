<template>
  <div class="exam-review-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-wrapper">
      <el-icon class="is-loading loading-icon"><Loading /></el-icon>
      <p>正在加载考试回顾...</p>
    </div>

    <!-- 主体内容 -->
    <template v-else>
      <header class="review-header">
        <div class="header-content">
          <div class="header-left">
            <el-button text @click="handleBack">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <h1 class="exam-title">{{ paper.name }}</h1>
            <span class="total-score">总分：{{ paper.totalScore }}</span>
          </div>
          <el-tag type="info" size="large">待批阅</el-tag>
        </div>
      </header>

      <main class="review-main">
        <!-- 左侧题目区 -->
        <div class="question-area">
          <div
            v-for="(q, index) in questions"
            :key="q.id"
            :ref="(el) => (questionRefs[index] = el)"
            class="question-card"
          >
            <div class="question-header">
              <span class="question-index">第 {{ index + 1 }} 题</span>
              <el-tag size="small" :type="typeTagColor(q.type)">
                {{ typeLabel(q.type) }}
              </el-tag>
              <span class="question-score">{{ q.score }} 分</span>
            </div>

            <p class="question-content">{{ q.content }}</p>

            <!-- 客观题：单选 / 多选 / 判断 -->
            <div v-if="q.type !== 'essay'" class="options-list">
              <div
                v-for="opt in q.options"
                :key="opt.label"
                :class="[
                  'option-item',
                  { selected: isStudentAnswer(q.id, opt.label) },
                ]"
              >
                <div class="option-left">
                  <span
                    :class="[
                      'option-circle',
                      { selected: isStudentAnswer(q.id, opt.label) },
                    ]"
                  >
                    {{ opt.label }}
                  </span>
                  <span class="option-text">{{ opt.content }}</span>
                </div>
                <el-icon
                  v-if="isStudentAnswer(q.id, opt.label)"
                  class="check-icon"
                >
                  <Check />
                </el-icon>
              </div>
            </div>

            <!-- 主观题：简答 -->
            <div v-else class="essay-area">
              <div class="essay-answer-display">
                {{ getStudentEssayAnswer(q.id) || "（未作答）" }}
              </div>
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
                @click="scrollToQuestion(index)"
                :class="['sheet-item', { answered: hasStudentAnswer(q.id) }]"
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
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Check, ArrowLeft, Loading } from "@element-plus/icons-vue";
import { getExamInfo, getExamPaper, getAnswerDetail } from "@/api/examTask";

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
const paper = ref({ name: "", totalScore: 0, stuPaperQuestionVOS: [] });
const questions = ref([]);
// 学生答题数据: { questionId: "A" 或 "A,B" 或 "文本内容" }
const studentAnswers = ref({});
const questionRefs = ref({});

// ========== 数据获取 ==========
const fetchData = async () => {
  loading.value = true;
  const examId = route.params.id;
  try {
    // 1. 获取考试信息
    await getExamInfo(examId);

    // 2. 获取该场考试的试卷快照
    const paperRes = await getExamPaper(examId);
    paper.value = paperRes.data;

    // 扁平化题目
    questions.value = paperRes.data.stuPaperQuestionVOS.map((pq) => ({
      sortOrder: pq.sortOrder,
      score: pq.score,
      id: pq.stuQuestionCompleteVO.id,
      content: pq.stuQuestionCompleteVO.content,
      type: pq.stuQuestionCompleteVO.type?.toLowerCase(),
      options: pq.stuQuestionCompleteVO.options || [],
    }));

    // 3. 获取学生答题详情
    const answerRes = await getAnswerDetail(examId);
    const answerList = answerRes.data || [];
    // 构建 { questionId: studentAnswer } 映射
    answerList.forEach((item) => {
      studentAnswers.value[item.questionId] = item.studentAnswer || "";
    });
  } catch (error) {
    console.error("加载考试回顾失败:", error);
  } finally {
    loading.value = false;
  }
};

// ========== 答题判断 ==========
// 客观题：判断某选项是否为学生的选择
const isStudentAnswer = (questionId, optionLabel) => {
  const answer = studentAnswers.value[questionId];
  if (!answer) return false;
  // studentAnswer 格式: "A" 或 "A,B" (逗号分隔，已排序)
  const selected = answer.split(",").map((s) => s.trim());
  return selected.includes(optionLabel);
};

// 主观题：获取学生的文本答案
const getStudentEssayAnswer = (questionId) => {
  return studentAnswers.value[questionId] || "";
};

// 判断题目是否已作答
const hasStudentAnswer = (questionId) => {
  const answer = studentAnswers.value[questionId];
  if (!answer) return false;
  // 客观题看是否有内容，主观题看是否为空字符串
  const q = questions.value.find((q) => q.id === questionId);
  if (q && q.type === "essay") {
    return answer.trim() !== "";
  }
  return answer.trim() !== "";
};

// ========== 导航 ==========
const scrollToQuestion = (index) => {
  const el = questionRefs.value[index];
  if (el) {
    el.scrollIntoView({ behavior: "smooth", block: "start" });
  }
};

const handleBack = () => {
  router.push("/exam/list");
};

// ========== 生命周期 ==========
onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.exam-review-container {
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
.review-header {
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

/* 主体布局 */
.review-main {
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
  min-width: 0;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.essay-answer-display {
  padding: 16px;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  color: #374151;
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 80px;
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

.sheet-item.answered {
  background-color: #dcfce7;
  color: #166534;
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

/* 响应式调整 */
@media (max-width: 768px) {
  .review-main {
    flex-direction: column;
  }

  .answer-sheet {
    width: 100%;
  }

  .sheet-card {
    position: static;
  }
}
</style>
