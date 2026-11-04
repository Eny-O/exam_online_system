<template>
  <div class="exam-result-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-wrapper">
      <el-icon class="is-loading loading-icon"><Loading /></el-icon>
      <p>正在加载考试结果...</p>
    </div>

    <div v-else class="content-wrapper">
      <!-- 返回按钮 -->
      <el-button type="primary" link @click="handleBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回考试列表
      </el-button>

      <!-- 结果概览卡片 -->
      <div class="result-card fade-in-up">
        <div class="header-section">
          <div
            class="trophy-icon-wrapper"
            :class="isPassed ? 'passed' : 'failed'"
          >
            <el-icon :size="48">
              <Trophy />
            </el-icon>
          </div>
          <h1 class="exam-title">{{ examResult.examName }}</h1>
          <div class="exam-time-info">
            <span>考试时间：{{ formatDateTime(examResult.examTime) }}</span>
            <span>提交时间：{{ formatDateTime(examResult.submitTime) }}</span>
          </div>
        </div>

        <!-- 统计数据网格 -->
        <div class="stats-grid">
          <div class="stat-item blue-theme">
            <div class="stat-value">{{ examResult.examScore ?? "--" }}</div>
            <div class="stat-label">得分 / {{ examResult.totalScore }}</div>
          </div>
          <div class="stat-item green-theme">
            <div class="stat-value">
              {{ correctCount }}/{{ questions.length }}
            </div>
            <div class="stat-label">答对题数</div>
          </div>
          <div class="stat-item purple-theme">
            <div class="stat-value">{{ actualDuration }}</div>
            <div class="stat-label">实际用时</div>
          </div>
          <div
            class="stat-item"
            :class="isPassed ? 'green-theme' : 'red-theme'"
          >
            <div class="stat-value">
              {{ statusText }}
            </div>
            <div class="stat-label">考试结果</div>
          </div>
        </div>

        <!-- 得分率进度条 -->
        <div class="progress-section">
          <div class="progress-header">
            <span class="progress-label">得分率</span>
            <span class="progress-percent">{{ scorePercentage }}%</span>
          </div>
          <el-progress
            :percentage="scorePercentage"
            :color="customColors"
            :stroke-width="12"
            :show-text="false"
            class="custom-progress-bar"
          />
        </div>
      </div>

      <!-- 答题详情列表（公共组件） -->
      <AnswerReview :questions="questions" :filterable="true" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ArrowLeft,
  Trophy,
  Loading,
} from "@element-plus/icons-vue";
import {
  getExamResult,
  getExamInfo,
  getStuAnswerReview,
} from "@/api/examTask";
import AnswerReview from "@/components/AnswerReview.vue";

const route = useRoute();
const router = useRouter();

// ========== 状态定义 ==========
const loading = ref(true);
const examResult = ref({
  examId: null,
  examName: "",
  totalScore: 0,
  examTime: "",
  examDuration: 0,
  submitTime: "",
  examScore: null,
});
const questions = ref([]);

// ========== 计算属性 ==========
// 答对题数（客观题 isCorrect === 1）
const correctCount = computed(() => {
  return questions.value.filter((q) => q.isCorrect === 1).length;
});

// 实际用时（根据考试开始时间和提交时间计算）
const actualDuration = computed(() => {
  const start = examResult.value.examTime;
  const end = examResult.value.submitTime;
  if (!start || !end) return "--";
  const diffMs = new Date(end) - new Date(start);
  if (diffMs <= 0) return "--";
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 60) return `${minutes}分钟`;
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  return mins > 0 ? `${hours}小时${mins}分钟` : `${hours}小时`;
});

// 得分率
const scorePercentage = computed(() => {
  if (!examResult.value.totalScore || examResult.value.totalScore === 0)
    return 0;
  const score = examResult.value.examScore ?? 0;
  return Math.round((score / examResult.value.totalScore) * 100);
});

// 是否通过（得分 >= 总分60%）
const isPassed = computed(() => {
  const score = examResult.value.examScore;
  const total = examResult.value.totalScore;
  if (score == null || !total) return false;
  return score >= total * 0.6;
});

// 考试结果文字
const statusText = computed(() => {
  if (examResult.value.examScore == null) return "待批阅";
  return isPassed.value ? "通过" : "不通过";
});

// Progress 颜色配置
const customColors = [
  { color: "#f56c6c", percentage: 20 },
  { color: "#e6a23c", percentage: 40 },
  { color: "#5cb87a", percentage: 60 },
  { color: "#1989fa", percentage: 80 },
  { color: "#6f7ad3", percentage: 100 },
];

// ========== 辅助方法 ==========
const formatDateTime = (dt) => {
  if (!dt) return "";
  return dt.replace("T", " ").substring(0, 16);
};

// ========== 数据获取 ==========
const fetchData = async () => {
  loading.value = true;
  const examId = route.params.id;

  try {
    // 并行获取考试结果和考试信息
    const [resultRes, infoRes] = await Promise.all([
      getExamResult(examId),
      getExamInfo(examId),
    ]);

    // 填充考试结果
    examResult.value = resultRes.data;

    // 获取答卷回顾（含标准答案与解析 + 学生作答，由后端合并）
    const reviewRes = await getStuAnswerReview(examId);
    questions.value = (reviewRes.data || []).map((v) => ({
      ...v,
      id: v.questionId,
    }));
  } catch (error) {
    console.error("加载考试结果失败:", error);
  } finally {
    loading.value = false;
  }
};

// ========== 方法 ==========
const handleBack = () => {
  router.push("/exam/list");
};

// ========== 生命周期 ==========
onMounted(() => {
  fetchData();
});
</script>

<style scoped>
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

/* 容器样式 */
.exam-result-container {
  min-height: 100vh;
  background-color: #f9fafb;
  padding: 24px;
}

.content-wrapper {
  max-width: 896px;
  margin: 0 auto;
}

/* 返回按钮 */
.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4b5563;
  margin-bottom: 24px;
  font-size: 16px;
}
.back-btn:hover {
  color: #111827;
}

/* 卡片通用样式 */
.result-card,
.detail-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
  padding: 32px;
  margin-bottom: 24px;
}

/* 头部区域 */
.header-section {
  text-align: center;
  margin-bottom: 32px;
}

.trophy-icon-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  margin-bottom: 16px;
}

.trophy-icon-wrapper.passed {
  background-color: #dcfce7;
  color: #16a34a;
}

.trophy-icon-wrapper.failed {
  background-color: #fee2e2;
  color: #dc2626;
}

.exam-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
}

.exam-time-info {
  color: #6b7280;
  font-size: 14px;
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 4px;
}

/* 统计网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-item {
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #4b5563;
}

/* 主题颜色 */
.blue-theme .stat-value {
  color: #2563eb;
}
.blue-theme {
  background-color: #eff6ff;
}

.green-theme .stat-value {
  color: #16a34a;
}
.green-theme {
  background-color: #f0fdf4;
}

.purple-theme .stat-value {
  color: #9333ea;
}
.purple-theme {
  background-color: #faf5ff;
}

.red-theme .stat-value {
  color: #dc2626;
}
.red-theme {
  background-color: #fef2f2;
}

/* 进度条区域 */
.progress-section {
  background-color: #f9fafb;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  width: 100%;
  box-sizing: border-box;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.progress-label {
  color: #374151;
  font-weight: 500;
}

.progress-percent {
  color: #374151;
  font-weight: 700;
}
.custom-progress-bar {
  width: 100%;
}

/* 详情部分 */
.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 24px 0;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-item {
  border-radius: 0 8px 8px 0;
  padding: 16px;
  border-left-width: 4px;
  border-left-style: solid;
}

.correct-border {
  border-left-color: #22c55e;
  background-color: #f0fdf4;
}

.wrong-border {
  border-left-color: #ef4444;
  background-color: #fef2f2;
}

.pending-border {
  border-left-color: #d1d5db;
  background-color: #f9fafb;
}

.question-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.status-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bg-green {
  background-color: #22c55e;
}
.bg-red {
  background-color: #ef4444;
}
.bg-gray {
  background-color: #9ca3af;
}

.question-info {
  flex: 1;
}

.question-text {
  font-weight: 500;
  color: #111827;
  margin: 0 0 8px 0;
  font-size: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.answer-details {
  font-size: 14px;
}

.answer-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  margin-top: 8px;
}

.label {
  color: #6b7280;
}

.value {
  font-weight: 500;
}

.text-green {
  color: #16a34a;
}
.text-red {
  color: #dc2626;
}

/* 紧凑选项列表 */
.options-compact {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}

.option-compact {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid transparent;
  font-size: 14px;
}

.opt-label {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  background-color: #f3f4f6;
  color: #6b7280;
  flex-shrink: 0;
}

.opt-text {
  color: #374151;
}

/* 学生选中的选项 */
.opt-selected {
  border-width: 2px;
}

.opt-correct {
  border-color: #22c55e;
  background-color: #f0fdf4;
}

.opt-correct .opt-label {
  background-color: #22c55e;
  color: #fff;
}

.opt-wrong {
  border-color: #ef4444;
  background-color: #fef2f2;
}

.opt-wrong .opt-label {
  background-color: #ef4444;
  color: #fff;
}

.opt-neutral {
  border-color: #93c5fd;
  background-color: #eff6ff;
}

.opt-neutral .opt-label {
  background-color: #3b82f6;
  color: #fff;
}

/* 简答回答展示 */
.essay-answer-display {
  padding: 12px;
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  color: #374151;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 60px;
  margin-top: 4px;
}

/* 批阅状态标签 */
.grade-pending,
.grade-partial {
  margin-top: 8px;
}

.score-section {
  text-align: right;
  flex-shrink: 0;
}

.score-value {
  font-size: 18px;
  font-weight: 700;
}

.score-total {
  color: #9ca3af;
  font-size: 12px;
}

/* 动画效果 */
.fade-in-up {
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
  transform: translateY(20px);
}

.fade-in-left {
  animation: fadeInLeft 0.5s ease-out forwards;
  opacity: 0;
  transform: translateX(-20px);
}

.delay-200 {
  animation-delay: 0.2s;
}

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInLeft {
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
