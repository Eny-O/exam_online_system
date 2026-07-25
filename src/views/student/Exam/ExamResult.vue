<template>
  <div class="exam-result-container">
    <div class="content-wrapper">
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
            :class="mockResult.passed ? 'passed' : 'failed'"
          >
            <el-icon :size="48">
              <Trophy />
            </el-icon>
          </div>
          <h1 class="exam-title">{{ mockResult.examName }}</h1>
          <p class="exam-date">{{ mockResult.examDate }}</p>
        </div>

        <!-- 统计数据网格 -->
        <div class="stats-grid">
          <div class="stat-item blue-theme">
            <div class="stat-value">{{ mockResult.score }}</div>
            <div class="stat-label">得分</div>
          </div>
          <div class="stat-item green-theme">
            <div class="stat-value">
              {{ mockResult.correctCount }}/{{ mockResult.totalQuestions }}
            </div>
            <div class="stat-label">答对题数</div>
          </div>
          <div class="stat-item purple-theme">
            <div class="stat-value">{{ mockResult.duration }}分钟</div>
            <div class="stat-label">用时</div>
          </div>
          <div
            class="stat-item"
            :class="mockResult.passed ? 'green-theme' : 'red-theme'"
          >
            <div class="stat-value">
              {{ mockResult.passed ? "通过" : "未通过" }}
            </div>
            <div class="stat-label">考试结果</div>
          </div>
        </div>

        <!-- 正确率进度条 -->
        <div class="progress-section">
          <div class="progress-header">
            <span class="progress-label">正确率</span>
            <span class="progress-percent">{{ accuracyPercentage }}%</span>
          </div>
          <div class="demo-progress">
            <el-progress
              :percentage="accuracyPercentage"
              :color="customColors"
              :stroke-width="12"
              :show-text="true"
              class="custom-progress-bar"
            />
          </div>
        </div>
      </div>

      <!-- 答题详情列表 -->
      <div class="detail-card fade-in-up delay-200">
        <h2 class="section-title">答题详情</h2>

        <div class="question-list">
          <div
            v-for="(q, index) in mockQuestions"
            :key="q.id"
            class="question-item fade-in-left"
            :style="{ animationDelay: `${index * 0.1}s` }"
            :class="q.isCorrect ? 'correct-border' : 'wrong-border'"
          >
            <div class="question-content">
              <div
                class="status-icon"
                :class="q.isCorrect ? 'bg-green' : 'bg-red'"
              >
                <el-icon :size="20" color="#fff">
                  <Check v-if="q.isCorrect" />
                  <Close v-else />
                </el-icon>
              </div>

              <div class="question-info">
                <h3 class="question-text">{{ index + 1 }}. {{ q.question }}</h3>

                <div class="answer-details">
                  <div class="answer-row">
                    <span class="label">你的答案:</span>
                    <span
                      class="value"
                      :class="q.isCorrect ? 'text-green' : 'text-red'"
                    >
                      {{ q.userAnswer }}
                    </span>
                  </div>

                  <div v-if="!q.isCorrect" class="answer-row">
                    <span class="label">正确答案:</span>
                    <span class="value text-green">{{ q.correctAnswer }}</span>
                  </div>

                  <div class="explanation-box">
                    {{ q.explanation }}
                  </div>
                </div>
              </div>

              <div class="score-section">
                <span
                  class="score-value"
                  :class="q.isCorrect ? 'text-green' : 'text-red'"
                >
                  +{{ q.score }}
                </span>
                <span class="score-total">/5</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ArrowLeft, Trophy, Check, Close } from "@element-plus/icons-vue";
import { ElProgress, ElButton, ElIcon } from "element-plus";

const router = useRouter();

// 模拟数据
const mockResult = ref({
  examName: "JavaScript 基础测试",
  examDate: "2024-01-15 14:30",
  duration: 45,
  totalScore: 100,
  score: 85,
  correctCount: 17,
  totalQuestions: 20,
  passScore: 60,
  passed: true,
});

const mockQuestions = ref([
  {
    id: 1,
    question: "React中useState的作用是什么？",
    userAnswer: "A",
    correctAnswer: "A",
    isCorrect: true,
    explanation: "useState是React的Hook，用于在函数组件中添加状态管理功能。",
    score: 5,
  },
  {
    id: 2,
    question: "JavaScript中const声明的变量可以重新赋值吗？",
    userAnswer: "A",
    correctAnswer: "B",
    isCorrect: false,
    explanation:
      "const声明的是常量，不能重新赋值，但如果是对象或数组，可以修改其属性或元素。",
    score: 0,
  },
  {
    id: 3,
    question: "CSS中flex-direction的默认值是什么？",
    userAnswer: "A",
    correctAnswer: "A",
    isCorrect: true,
    explanation: "flex-direction默认值为row，表示主轴方向为水平方向。",
    score: 5,
  },
]);

// 计算正确率
const accuracyPercentage = computed(() => {
  if (mockResult.value.totalQuestions === 0) return 0;
  return Math.round(
    (mockResult.value.correctCount / mockResult.value.totalQuestions) * 100
  );
});

// Element Plus Progress 颜色配置
const customColors = [
  { color: "#f56c6c", percentage: 20 },
  { color: "#e6a23c", percentage: 40 },
  { color: "#5cb87a", percentage: 60 },
  { color: "#1989fa", percentage: 80 },
  { color: "#6f7ad3", percentage: 100 },
];

// 方法
const handleBack = () => {
  router.push("/exams");
};

onMounted(() => {
  // 可以在这里根据 route.params.id 获取真实数据
  console.log("Exam Result Page Loaded");
});
</script>

<style scoped>
/* 容器样式 */
.exam-result-container {
  min-height: 100vh;
  background-color: #f9fafb; /* bg-gray-50 */
  padding: 24px;
}

.content-wrapper {
  max-width: 896px; /* max-w-4xl */
  margin: 0 auto;
}

/* 返回按钮 */
.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4b5563; /* text-gray-600 */
  margin-bottom: 24px;
  font-size: 16px;
}
.back-btn:hover {
  color: #111827; /* hover:text-gray-900 */
}

/* 卡片通用样式 */
.result-card,
.detail-card {
  background-color: #ffffff;
  border-radius: 12px; /* rounded-xl */
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06); /* shadow-sm */
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
  width: 96px; /* w-24 */
  height: 96px; /* h-24 */
  border-radius: 50%;
  margin-bottom: 16px;
}

.trophy-icon-wrapper.passed {
  background-color: #dcfce7; /* bg-green-100 */
  color: #16a34a; /* text-green-600 */
}

.trophy-icon-wrapper.failed {
  background-color: #fee2e2; /* bg-red-100 */
  color: #dc2626; /* text-red-600 */
}

.exam-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827; /* text-gray-900 */
  margin: 0 0 8px 0;
}

.exam-date {
  color: #6b7280; /* text-gray-500 */
  margin: 0;
}

/* 统计网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-item {
  border-radius: 8px; /* rounded-lg */
  padding: 16px;
  text-align: center;
}

.stat-value {
  font-size: 30px; /* text-3xl */
  font-weight: 700;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px; /* text-sm */
  color: #4b5563; /* text-gray-600 */
}

/* 主题颜色 */
.blue-theme .stat-value {
  color: #2563eb;
} /* text-blue-600 */
.blue-theme {
  background-color: #eff6ff;
} /* bg-blue-50 */

.green-theme .stat-value {
  color: #16a34a;
} /* text-green-600 */
.green-theme {
  background-color: #f0fdf4;
} /* bg-green-50 */

.purple-theme .stat-value {
  color: #9333ea;
} /* text-purple-600 */
.purple-theme {
  background-color: #faf5ff;
} /* bg-purple-50 */

.red-theme .stat-value {
  color: #dc2626;
} /* text-red-600 */
.red-theme {
  background-color: #fef2f2;
} /* bg-red-50 */

/* 进度条区域 */
.progress-section {
  background-color: #f9fafb; /* bg-gray-50 */
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
  background-color: red;
}

.progress-label {
  color: #374151; /* text-gray-700 */
  font-weight: 500;
}

.progress-percent {
  color: #374151;
  font-weight: 700;
}
.custom-progress-bar {
  width: 100%; /* 确保占满父容器宽度 */
  --el-progress-bezier: cubic-bezier(0.4, 0, 0.2, 1);
}
.demo-progress .el-progress--line {
  margin-bottom: 15px;
  max-width: 600px;
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
  border-radius: 0 8px 8px 0; /* rounded-r-lg */
  padding: 16px;
  border-left-width: 4px;
  border-left-style: solid;
}

.correct-border {
  border-left-color: #22c55e; /* border-green-500 */
  background-color: #f0fdf4; /* bg-green-50 */
}

.wrong-border {
  border-left-color: #ef4444; /* border-red-500 */
  background-color: #fef2f2; /* bg-red-50 */
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

.question-info {
  flex: 1;
}

.question-text {
  font-weight: 500;
  color: #111827;
  margin: 0 0 8px 0;
  font-size: 16px;
}

.answer-details {
  font-size: 14px;
}

.answer-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.label {
  color: #6b7280; /* text-gray-500 */
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

.explanation-box {
  color: #4b5563; /* text-gray-600 */
  margin-top: 8px;
  padding: 12px;
  background-color: #ffffff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.score-section {
  text-align: right;
}

.score-value {
  font-size: 18px;
  font-weight: 700;
}

.score-total {
  color: #9ca3af; /* text-gray-400 */
  font-size: 12px;
}

/* 动画效果 (替代 framer-motion) */
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
