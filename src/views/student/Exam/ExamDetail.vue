<template>
  <div class="exam-detail-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-wrapper">
      <el-icon class="is-loading loading-icon"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="!exam" class="error-wrapper">
      <el-icon class="error-icon"><CircleCloseFilled /></el-icon>
      <p class="error-text">考试不存在或已过期</p>
      <el-button type="primary" @click="router.back()">返回</el-button>
    </div>

    <!-- 主要内容 -->
    <motion-div v-else class="exam-card-wrapper fade-in-up">
      <div class="exam-card">
        <!-- 头部渐变区域 -->
        <div class="card-header">
          <h1 class="exam-title">{{ exam.title }}</h1>
          <p class="exam-desc">{{ exam.description }}</p>
        </div>

        <div class="card-body">
          <!-- 统计信息网格 -->
          <div class="stats-grid">
            <div class="stat-item">
              <el-icon class="stat-icon blue"><Clock /></el-icon>
              <p class="stat-label">考试时长</p>
              <p class="stat-value">{{ exam.duration }}分钟</p>
            </div>
            <div class="stat-item">
              <el-icon class="stat-icon green"><Document /></el-icon>
              <p class="stat-label">题目数量</p>
              <p class="stat-value">{{ exam.totalQuestions }}题</p>
            </div>
            <div class="stat-item">
              <el-icon class="stat-icon purple"><Check /></el-icon>
              <p class="stat-label">总分</p>
              <p class="stat-value">{{ exam.totalScore }}分</p>
            </div>
            <div class="stat-item">
              <el-icon class="stat-icon orange"><Warning /></el-icon>
              <p class="stat-label">及格线</p>
              <p class="stat-value">{{ exam.passScore }}分</p>
            </div>
          </div>

          <!-- 考试规则 -->
          <div class="rules-section">
            <h3 class="section-title">考试规则</h3>
            <ul class="rules-list">
              <li
                v-for="(rule, index) in exam.rules"
                :key="index"
                class="rule-item"
              >
                <span class="rule-number">{{ index + 1 }}</span>
                <span class="rule-content">{{ rule }}</span>
              </li>
            </ul>
          </div>

          <!-- 开始按钮 -->
          <div class="action-area">
            <el-button
              type="primary"
              size="large"
              class="start-btn"
              @click="handleStartExam"
            >
              <el-icon><VideoPlay /></el-icon>
              开始考试
            </el-button>
          </div>
        </div>
      </div>
    </motion-div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Clock,
  Document,
  Check,
  Warning,
  Loading,
  CircleCloseFilled,
  VideoPlay,
} from "@element-plus/icons-vue";
// import { ElMessage } from "element-plus";

const route = useRoute();
const router = useRouter();

// 状态定义
const loading = ref(true);
const exam = ref(null);

// 模拟数据获取
const fetchExamDetail = async () => {
  const id = route.params.id;

  // 模拟网络请求延迟
  setTimeout(() => {
    // 这里可以根据 id 筛选真实数据，暂时使用固定 mock
    if (id) {
      exam.value = {
        id: id,
        title: "JavaScript 基础测试",
        description: "考察 JavaScript 基础语法、数据类型、函数等核心概念",
        duration: 60,
        totalQuestions: 20,
        totalScore: 100,
        passScore: 60,
        rules: [
          "考试时长为60分钟，超时自动提交",
          "每题5分，共20题，满分100分",
          "及格分数为60分",
          "考试过程中请勿刷新页面",
          "请确保网络连接稳定",
        ],
      };
    } else {
      exam.value = null;
    }
    loading.value = false;
  }, 500);
};

// 方法
const handleStartExam = () => {
  // 跳转到考试页面
  router.push(`/exam/${exam.value.id}/take`);
};

// 生命周期
onMounted(() => {
  fetchExamDetail();
});
</script>

<style scoped>
.exam-detail-container {
  min-height: 100vh;
  background-color: #f9fafb;
  padding: 40px 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

/* 加载与错误状态 */
.loading-wrapper,
.error-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
  color: #6b7280;
}

.loading-icon {
  font-size: 32px;
  color: #2563eb;
  margin-bottom: 16px;
}

.error-icon {
  font-size: 48px;
  color: #ef4444;
  margin-bottom: 16px;
}

.error-text {
  margin-bottom: 24px;
  font-size: 16px;
}

/* 卡片容器 */
.exam-card-wrapper {
  width: 100%;
  max-width: 896px; /* max-w-4xl */
}

.exam-card {
  background-color: white;
  border-radius: 12px; /* rounded-xl */
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05); /* shadow-sm */
  border: 1px solid #e5e7eb; /* border-gray-200 */
  overflow: hidden;
}

/* 卡片头部渐变 */
.card-header {
  background: linear-gradient(
    to right,
    #2563eb,
    #1d4ed8
  ); /* from-blue-600 to-blue-700 */
  padding: 32px 24px; /* px-6 py-8 */
  color: white;
}

.exam-title {
  font-size: 24px; /* text-2xl */
  font-weight: 700; /* font-bold */
  margin: 0 0 8px 0; /* mb-2 */
}

.exam-desc {
  color: #dbeafe; /* text-blue-100 */
  margin: 0;
  font-size: 16px;
}

/* 卡片主体 */
.card-body {
  padding: 24px; /* p-6 */
}

/* 统计网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px; /* gap-4 */
  margin-bottom: 32px; /* mb-8 */
}

@media (min-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(4, 1fr); /* md:grid-cols-4 */
  }
}

.stat-item {
  background-color: #f9fafb; /* bg-gray-50 */
  border-radius: 8px; /* rounded-lg */
  padding: 16px; /* p-4 */
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-icon {
  font-size: 24px; /* w-6 h-6 */
  margin-bottom: 8px; /* mb-2 */
}

.stat-icon.blue {
  color: #2563eb;
}
.stat-icon.green {
  color: #16a34a;
}
.stat-icon.purple {
  color: #9333ea;
}
.stat-icon.orange {
  color: #ea580c;
}

.stat-label {
  font-size: 14px; /* text-sm */
  color: #4b5563; /* text-gray-600 */
  margin: 0 0 4px 0;
}

.stat-value {
  font-size: 18px; /* text-lg */
  font-weight: 600; /* font-semibold */
  color: #111827;
  margin: 0;
}

/* 规则部分 */
.rules-section {
  margin-bottom: 32px; /* mb-8 */
}

.section-title {
  font-size: 18px; /* text-lg */
  font-weight: 600; /* font-semibold */
  color: #111827; /* text-gray-900 */
  margin: 0 0 16px 0; /* mb-4 */
}

.rules-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px; /* space-y-3 */
}

.rule-item {
  display: flex;
  align-items: flex-start;
  gap: 12px; /* gap-3 */
}

.rule-number {
  flex-shrink: 0;
  width: 24px; /* w-6 */
  height: 24px; /* h-6 */
  background-color: #dbeafe; /* bg-blue-100 */
  color: #2563eb; /* text-blue-600 */
  border-radius: 50%; /* rounded-full */
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px; /* text-sm */
  font-weight: 500; /* font-medium */
}

.rule-content {
  color: #374151; /* text-gray-700 */
  line-height: 1.5;
  padding-top: 2px;
}

/* 操作区域 */
.action-area {
  display: flex;
  justify-content: center;
}

.start-btn {
  padding-left: 32px;
  padding-right: 32px;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
}

.start-btn :deep(.el-icon) {
  margin-right: 8px;
}

/* 动画 */
.fade-in-up {
  animation: fadeInUp 0.5s ease-out forwards;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
