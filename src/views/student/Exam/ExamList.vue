<template>
  <div class="exam-list-container">
    <div class="content-wrapper">
      <!-- 标题部分 -->
      <div class="header-section fade-in">
        <h1 class="title">考试中心</h1>
        <p class="subtitle">查看和管理您的所有考试</p>
      </div>

      <!-- 搜索和过滤栏 -->
      <el-card class="filter-card fade-in-delay" shadow="hover">
        <div class="filter-row">
          <el-input
            v-model="searchQuery"
            placeholder="搜索考试名称或科目..."
            prefix-icon="Search"
            class="search-input"
            clearable
          />
          <div class="filter-buttons">
            <el-button
              v-for="status in statusOptions"
              :key="status.value"
              :type="filterStatus === status.value ? 'primary' : 'default'"
              @click="filterStatus = status.value"
              class="filter-btn"
            >
              {{ status.label }}
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 考试列表 -->
      <div class="exam-grid">
        <el-card
          v-for="(exam, index) in filteredExams"
          :key="exam.id"
          class="exam-card fade-in-up"
          :style="{ animationDelay: `${index * 0.05}s` }"
          shadow="hover"
          @click="handleExamAction(exam)"
        >
          <div class="exam-content">
            <div class="exam-info">
              <div class="title-row">
                <h3 class="exam-title">{{ exam.title }}</h3>
                <el-tag
                  :type="getStatusTagType(exam.status)"
                  effect="light"
                  class="status-tag"
                >
                  {{ getStatusLabel(exam.status) }}
                </el-tag>
              </div>
              <div class="meta-row">
                <span class="meta-item">
                  <el-icon><Filter /></el-icon>
                  {{ exam.subject }}
                </span>
                <span class="meta-item">
                  <el-icon><Clock /></el-icon>
                  {{ exam.duration }}分钟
                </span>
                <span class="meta-item">
                  <el-icon><Calendar /></el-icon>
                  {{ exam.startTime }}
                </span>
                <span class="meta-item"> {{ exam.totalQuestions }}道题 </span>
              </div>
            </div>

            <div class="action-area">
              <div v-if="exam.score !== undefined" class="score-display">
                <div class="score-value">{{ exam.score }}</div>
                <div class="score-label">分</div>
              </div>
              <el-button
                :type="getButtonStatusType(exam.status)"
                @click.stop="handleExamAction(exam)"
              >
                {{ getButtonLabel(exam.status) }}
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredExams.length === 0" class="empty-state fade-in">
        <div class="empty-icon-wrapper">
          <el-icon :size="32"><Search /></el-icon>
        </div>
        <p>没有找到匹配的考试</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import { Search, Filter, Clock, Calendar } from "@element-plus/icons-vue";

// 模拟数据
const mockExams = [
  {
    id: "1",
    title: "高等数学期中考试",
    subject: "数学",
    duration: 120,
    totalQuestions: 50,
    startTime: "2024-01-15 09:00",
    status: "ongoing",
  },
  {
    id: "2",
    title: "大学英语四级模拟",
    subject: "英语",
    duration: 90,
    totalQuestions: 80,
    startTime: "2024-01-16 14:00",
    status: "upcoming",
  },
  {
    id: "3",
    title: "计算机基础测试",
    subject: "计算机",
    duration: 60,
    totalQuestions: 40,
    startTime: "2024-01-10 10:00",
    status: "completed",
    score: 85,
  },
  {
    id: "4",
    title: "线性代数期末考试",
    subject: "数学",
    duration: 150,
    totalQuestions: 60,
    startTime: "2024-01-20 08:00",
    status: "upcoming",
  },
  {
    id: "5",
    title: "数据结构期中考试",
    subject: "计算机",
    duration: 120,
    totalQuestions: 45,
    startTime: "2024-01-08 09:00",
    status: "completed",
    score: 92,
  },
];

const router = useRouter();
const searchQuery = ref("");
const filterStatus = ref("all");

const statusOptions = [
  { value: "all", label: "全部" },
  { value: "ongoing", label: "进行中" },
  { value: "upcoming", label: "即将开始" },
  { value: "completed", label: "已完成" },
];

// 计算属性：过滤考试列表
const filteredExams = computed(() => {
  return mockExams.filter((exam) => {
    const matchesSearch =
      exam.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      exam.subject.toLowerCase().includes(searchQuery.value.toLowerCase());
    const matchesFilter =
      filterStatus.value === "all" || exam.status === filterStatus.value;
    return matchesSearch && matchesFilter;
  });
});

// 辅助函数：获取状态标签类型
const getStatusTagType = (status) => {
  switch (status) {
    case "ongoing":
      return "success"; // 绿色
    case "upcoming":
      return "primary"; // 蓝色
    case "completed":
      return "info"; // 灰色
    default:
      return "info";
  }
};

// 辅助函数：获取状态标签文本
const getStatusLabel = (status) => {
  switch (status) {
    case "ongoing":
      return "进行中";
    case "upcoming":
      return "即将开始";
    case "completed":
      return "已完成";
    default:
      return "未知";
  }
};

// 辅助函数：获取按钮类型
const getButtonStatusType = (status) => {
  switch (status) {
    case "ongoing":
      return "success";
    case "upcoming":
      return "primary";
    case "completed":
      return "default";
    default:
      return "default";
  }
};

// 辅助函数：获取按钮文本
const getButtonLabel = (status) => {
  switch (status) {
    case "ongoing":
      return "进入考试";
    case "upcoming":
      return "查看详情";
    case "completed":
      return "查看成绩";
    default:
      return "查看";
  }
};

// 处理点击事件
const handleExamAction = (exam) => {
  if (exam.status === "ongoing") {
    router.push(`/exam/${exam.id}/take`);
  } else if (exam.status === "upcoming") {
    router.push(`/exam/detail/${exam.id}`);
  } else {
    router.push(`/exam/${exam.id}/result`);
  }
};
</script>

<style scoped>
/* 容器样式 */
.exam-list-container {
  min-height: 100vh;
  background-color: #f9fafb; /* bg-gray-50 */
  padding: 24px; /* p-6 */
}

.content-wrapper {
  max-width: 1200px; /* max-w-6xl */
  margin: 0 auto;
}

/* 头部样式 */
.header-section {
  margin-bottom: 32px; /* mb-8 */
}

.title {
  font-size: 30px; /* text-3xl */
  font-weight: bold;
  color: #111827; /* text-gray-900 */
  margin-bottom: 8px;
  margin-top: 0;
}

.subtitle {
  color: #4b5563; /* text-gray-600 */
  margin: 0;
}

/* 过滤卡片样式 */
.filter-card {
  margin-bottom: 24px; /* mb-6 */
  border-radius: 12px; /* rounded-xl */
}

.filter-row {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (min-width: 768px) {
  .filter-row {
    flex-direction: row;
  }
}

.search-input {
  flex: 1;
}

.filter-buttons {
  display: flex;
  gap: 8px; /* gap-2 */
  flex-wrap: wrap;
}

.filter-btn {
  /* 自定义按钮样式以匹配原设计 */
  border-radius: 8px; /* rounded-lg */
  padding: 12px 16px; /* py-3 px-4 */
  font-weight: 500;
}

/* 考试列表网格 */
.exam-grid {
  display: grid;
  gap: 16px; /* gap-4 */
}

.exam-card {
  border-radius: 12px; /* rounded-xl */
  cursor: pointer;
  transition: all 0.3s ease;
}

.exam-card:hover {
  transform: scale(1.01); /* whileHover scale */
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
    0 2px 4px -1px rgba(0, 0, 0, 0.06); /* hover:shadow-md */
}

.exam-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.exam-info {
  flex: 1;
  min-width: 200px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.exam-title {
  font-size: 18px; /* text-lg */
  font-weight: 600; /* font-semibold */
  color: #111827; /* text-gray-900 */
  margin: 0;
}

.status-tag {
  border-radius: 9999px; /* rounded-full */
  font-size: 14px; /* text-sm */
  font-weight: 500; /* font-medium */
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 24px; /* gap-6 */
  color: #4b5563; /* text-gray-600 */
  font-size: 14px; /* text-sm */
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-area {
  display: flex;
  align-items: center;
  gap: 16px; /* gap-4 */
}

.score-display {
  text-align: right;
}

.score-value {
  font-size: 24px; /* text-2xl */
  font-weight: bold;
  color: #2563eb; /* text-blue-600 */
  line-height: 1;
}

.score-label {
  font-size: 12px; /* text-xs */
  color: #6b7280; /* text-gray-500 */
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 48px 0; /* py-12 */
}

.empty-icon-wrapper {
  width: 64px; /* w-16 */
  height: 64px; /* h-16 */
  background-color: #f3f4f6; /* bg-gray-100 */
  border-radius: 50%; /* rounded-full */
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px; /* mx-auto mb-4 */
  color: #9ca3af; /* text-gray-400 */
}

.empty-state p {
  color: #6b7280; /* text-gray-500 */
}

/* 简单动画模拟 */
.fade-in {
  animation: fadeIn 0.5s ease-out forwards;
}

.fade-in-delay {
  animation: fadeIn 0.5s ease-out 0.1s forwards;
  opacity: 0;
}

.fade-in-up {
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
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
