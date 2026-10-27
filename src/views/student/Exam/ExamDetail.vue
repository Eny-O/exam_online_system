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
          <h1 class="exam-title">{{ exam.examName }}</h1>
          <p class="exam-desc">
            {{ formatDateTime(exam.startTime) }} ~ {{ getEndTime(exam) }}
          </p>
        </div>

        <div class="card-body">
          <!-- 统计信息网格 -->
          <div class="stats-grid">
            <div class="stat-item">
              <el-icon class="stat-icon blue"><Clock /></el-icon>
              <p class="stat-label">考试时长</p>
              <p class="stat-value">{{ exam.duration }} 分钟</p>
            </div>
            <div class="stat-item">
              <el-icon class="stat-icon green"><Calendar /></el-icon>
              <p class="stat-label">开始时间</p>
              <p class="stat-value">{{ formatDateTime(exam.startTime) }}</p>
            </div>
            <div class="stat-item">
              <el-icon class="stat-icon purple"><Check /></el-icon>
              <p class="stat-label">考试状态</p>
              <el-tag :type="getStatusType(getRealStatus(exam))">
                {{ getStatusText(getRealStatus(exam)) }}
              </el-tag>
            </div>
          </div>

          <!-- 考试须知 -->
          <div class="rules-section">
            <h3 class="section-title">考试须知</h3>
            <ul class="rules-list">
              <li class="rule-item">
                <span class="rule-number">1</span>
                <span class="rule-content"
                  >考试时长为 {{ exam.duration }} 分钟，超时自动提交</span
                >
              </li>
              <li class="rule-item">
                <span class="rule-number">2</span>
                <span class="rule-content"
                  >请确保网络连接稳定，考试过程中请勿刷新页面</span
                >
              </li>
              <li class="rule-item">
                <span class="rule-number">3</span>
                <span class="rule-content"
                  >考试开始后可随时提交，提交后不可修改答案</span
                >
              </li>
              <li class="rule-item">
                <span class="rule-number">4</span>
                <span class="rule-content">如有疑问请联系任课教师</span>
              </li>
            </ul>
          </div>

          <!-- 返回按钮 -->
          <div class="action-area">
            <el-button
              type="primary"
              size="large"
              class="back-btn"
              @click="router.back()"
            >
              <el-icon><ArrowLeft /></el-icon>
              返回列表
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
  Calendar,
  Check,
  Loading,
  CircleCloseFilled,
  ArrowLeft,
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { getExamInfo } from "@/api/examTask";

const route = useRoute();
const router = useRouter();

// ========== 状态映射 ==========
const STATUS_TEXT = {
  0: "未发布",
  1: "已发布",
  2: "未开始",
  3: "进行中",
  4: "待批阅",
  5: "批阅完成",
};

const STATUS_TYPE = {
  0: "info",
  1: "warning",
  2: "",
  3: "success",
  4: "warning",
  5: "success",
};

// 当 status === 1（已发布）时，根据时间计算真实状态
const getRealStatus = (row) => {
  if (row.status !== 1) return row.status;
  const now = new Date();
  const start = new Date(row.startTime);
  const end = new Date(start.getTime() + row.duration * 60 * 1000);
  if (now < start) return 2;
  if (now < end) return 3;
  return 4;
};

const getStatusText = (status) => STATUS_TEXT[status] ?? "未知";
const getStatusType = (status) => STATUS_TYPE[status] ?? "info";

// ========== 时间格式化 ==========
const pad = (n) => String(n).padStart(2, "0");

const formatDateTime = (dateStr) => {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
    d.getHours()
  )}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const getEndTime = (exam) => {
  const start = new Date(exam.startTime);
  const end = new Date(start.getTime() + exam.duration * 60 * 1000);
  return formatDateTime(end.toISOString().slice(0, 19).replace("T", " "));
};

// ========== 数据获取 ==========
const loading = ref(true);
const exam = ref(null);

const fetchExamDetail = async () => {
  loading.value = true;
  try {
    const res = await getExamInfo(route.params.id);
    exam.value = res.data;
  } catch (error) {
    console.error("获取考试详情失败:", error);
    exam.value = null;
  } finally {
    loading.value = false;
  }
};

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
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-item {
  background-color: #f9fafb;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-icon {
  font-size: 24px;
  margin-bottom: 8px;
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

.back-btn {
  padding-left: 32px;
  padding-right: 32px;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
}

.back-btn :deep(.el-icon) {
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
