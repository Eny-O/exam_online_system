<template>
  <div class="result-view-container">
    <div class="content-wrapper">
      <!-- 标题部分 -->
      <div class="header-section fade-in">
        <h1 class="page-title">成绩查询</h1>
        <p class="page-subtitle">查看您的所有考试成绩和排名</p>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-grid fade-in-delay-1">
        <div class="stat-card blue-theme">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">平均分</p>
              <p class="stat-value">{{ averageScore }}</p>
            </div>
            <div class="stat-icon-bg">
              <TrendCharts class="stat-icon" />
            </div>
          </div>
        </div>
        <div class="stat-card green-theme">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">通过考试</p>
              <p class="stat-value">{{ passedCount }}/{{ totalCount }}</p>
            </div>
            <div class="stat-icon-bg">
              <Trophy class="stat-icon" />
            </div>
          </div>
        </div>
        <div class="stat-card purple-theme">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">最高分</p>
              <p class="stat-value">{{ highestScore }}</p>
            </div>
            <div class="stat-icon-bg">
              <Document class="stat-icon" />
            </div>
          </div>
        </div>
      </div>

      <!-- 搜索和过滤栏 -->
      <div class="filter-section fade-in-delay-2">
        <div class="filter-row">
          <div class="search-input-wrapper">
            <el-input
              v-model="searchQuery"
              placeholder="搜索考试名称..."
              prefix-icon="Search"
              class="custom-search-input"
            />
          </div>
          <div class="subject-filters">
            <el-button
              v-for="subject in subjects"
              :key="subject"
              :type="selectedSubject === subject ? 'primary' : 'default'"
              @click="selectedSubject = subject"
              class="subject-btn"
            >
              {{ subject }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 成绩表格 -->
      <div class="table-section fade-in-delay-3">
        <el-table
          :data="filteredResults"
          style="width: 100%"
          :header-cell-style="{
            background: '#f9fafb',
            color: '#6b7280',
            fontWeight: '500',
          }"
          row-class-name="custom-table-row"
        >
          <el-table-column prop="title" label="考试名称" min-width="180">
            <template #default="scope">
              <span class="font-medium">{{ scope.row.title }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="subject" label="科目" width="100" />

          <el-table-column prop="date" label="考试日期" width="120">
            <template #default="scope">
              <div class="date-cell">
                <Calendar class="date-icon" />
                <span>{{ scope.row.date }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="score" label="得分" width="120">
            <template #default="scope">
              <span :class="getScoreClass(scope.row.score)">
                {{ scope.row.score }}/{{ scope.row.totalScore }}
              </span>
            </template>
          </el-table-column>

          <el-table-column prop="rank" label="排名" width="100">
            <template #default="scope"> 第 {{ scope.row.rank }} 名 </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag
                :type="scope.row.status === 'passed' ? 'success' : 'danger'"
                effect="light"
                round
              >
                {{ scope.row.status === "passed" ? "通过" : "未通过" }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 空状态 -->
        <div v-if="filteredResults.length === 0" class="empty-state">
          <Search class="empty-icon" />
          <p>没有找到匹配的成绩记录</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import {
  TrendCharts,
  Trophy,
  Document,
  Search,
  Calendar,
} from "@element-plus/icons-vue";

// 模拟数据
const mockResults = [
  {
    id: "1",
    title: "高等数学期中考试",
    subject: "数学",
    date: "2024-01-15",
    score: 85,
    totalScore: 100,
    rank: 12,
    status: "passed",
  },
  {
    id: "2",
    title: "大学英语四级模拟",
    subject: "英语",
    date: "2024-01-10",
    score: 78,
    totalScore: 100,
    rank: 25,
    status: "passed",
  },
  {
    id: "3",
    title: "计算机基础测试",
    subject: "计算机",
    date: "2024-01-08",
    score: 92,
    totalScore: 100,
    rank: 5,
    status: "passed",
  },
  {
    id: "4",
    title: "线性代数期末考试",
    subject: "数学",
    date: "2024-01-05",
    score: 55,
    totalScore: 100,
    rank: 45,
    status: "failed",
  },
  {
    id: "5",
    title: "数据结构期中考试",
    subject: "计算机",
    date: "2024-01-03",
    score: 88,
    totalScore: 100,
    rank: 8,
    status: "passed",
  },
];

const subjects = ["全部", "数学", "英语", "计算机"];

// 响应式状态
const searchQuery = ref("");
const selectedSubject = ref("全部");

// 计算属性
const filteredResults = computed(() => {
  return mockResults.filter((result) => {
    const matchesSearch = result.title
      .toLowerCase()
      .includes(searchQuery.value.toLowerCase());
    const matchesSubject =
      selectedSubject.value === "全部" ||
      result.subject === selectedSubject.value;
    return matchesSearch && matchesSubject;
  });
});

const averageScore = computed(() => {
  if (mockResults.length === 0) return 0;
  const sum = mockResults.reduce((sum, r) => sum + r.score, 0);
  return Math.round(sum / mockResults.length);
});

const passedCount = computed(() => {
  return mockResults.filter((r) => r.status === "passed").length;
});

const totalCount = computed(() => mockResults.length);

const highestScore = computed(() => {
  if (mockResults.length === 0) return 0;
  return Math.max(...mockResults.map((r) => r.score));
});

// 方法
const getScoreClass = (score) => {
  if (score >= 80) return "score-high";
  if (score >= 60) return "score-medium";
  return "score-low";
};
</script>

<style scoped>
.result-view-container {
  min-height: 100vh;
  background-color: #f9fafb; /* bg-gray-50 */
  padding: 24px; /* p-6 */
}

.content-wrapper {
  max-width: 1152px; /* max-w-6xl */
  margin: 0 auto;
}

/* 标题样式 */
.header-section {
  margin-bottom: 32px; /* mb-8 */
}

.page-title {
  font-size: 30px; /* text-3xl */
  font-weight: 700; /* font-bold */
  color: #111827; /* text-gray-900 */
  margin-bottom: 8px; /* mb-2 */
  margin-top: 0;
}

.page-subtitle {
  color: #4b5563; /* text-gray-600 */
  margin: 0;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 16px; /* gap-4 */
  margin-bottom: 24px; /* mb-6 */
}

@media (min-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr); /* md:grid-cols-3 */
  }
}

.stat-card {
  background-color: white;
  border-radius: 12px; /* rounded-xl */
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05); /* shadow-sm */
  padding: 24px; /* p-6 */
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  color: #6b7280; /* text-gray-500 */
  font-size: 14px; /* text-sm */
  margin: 0 0 4px 0;
}

.stat-value {
  font-size: 30px; /* text-3xl */
  font-weight: 700; /* font-bold */
  margin: 0;
}

.stat-icon-bg {
  width: 48px; /* w-12 */
  height: 48px; /* h-12 */
  border-radius: 8px; /* rounded-lg */
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon {
  width: 24px; /* w-6 */
  height: 24px; /* h-6 */
}

/* 主题颜色 */
.blue-theme .stat-value {
  color: #2563eb;
} /* text-blue-600 */
.blue-theme .stat-icon-bg {
  background-color: #dbeafe;
} /* bg-blue-100 */
.blue-theme .stat-icon {
  color: #2563eb;
}

.green-theme .stat-value {
  color: #16a34a;
} /* text-green-600 */
.green-theme .stat-icon-bg {
  background-color: #dcfce7;
} /* bg-green-100 */
.green-theme .stat-icon {
  color: #16a34a;
}

.purple-theme .stat-value {
  color: #9333ea;
} /* text-purple-600 */
.purple-theme .stat-icon-bg {
  background-color: #f3e8ff;
} /* bg-purple-100 */
.purple-theme .stat-icon {
  color: #9333ea;
}

/* 筛选区域 */
.filter-section {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  padding: 24px;
  margin-bottom: 24px;
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

.search-input-wrapper {
  flex: 1;
  position: relative;
}

.custom-search-input :deep(.el-input__wrapper) {
  padding-left: 40px; /* 为图标留空间 */
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e5e7eb inset; /* border border-gray-200 */
}

.custom-search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #d1d5db inset;
}

.custom-search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #3b82f6 inset !important; /* focus:ring-2 focus:ring-blue-500 */
}

.subject-filters {
  display: flex;
  gap: 8px; /* gap-2 */
}

.subject-btn {
  padding: 12px 16px; /* px-4 py-3 */
  border-radius: 8px; /* rounded-lg */
  font-weight: 500; /* font-medium */
}

/* 表格区域 */
.table-section {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.font-medium {
  font-weight: 500;
  color: #111827;
}

.date-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #4b5563;
}

.date-icon {
  width: 16px;
  height: 16px;
}

.score-high {
  font-weight: 700;
  color: #16a34a; /* text-green-600 */
}

.score-medium {
  font-weight: 700;
  color: #ca8a04; /* text-yellow-600 */
}

.score-low {
  font-weight: 700;
  color: #dc2626; /* text-red-600 */
}

.empty-state {
  text-align: center;
  padding: 48px 0; /* py-12 */
  color: #6b7280;
}

.empty-icon {
  width: 48px;
  height: 48px;
  color: #d1d5db; /* text-gray-300 */
  margin: 0 auto 16px auto;
  display: block;
}

/* 简单动画替代 framer-motion */
.fade-in {
  animation: fadeInUp 0.5s ease-out forwards;
}

.fade-in-delay-1 {
  animation: fadeInUp 0.5s ease-out 0.1s forwards;
  opacity: 0;
}

.fade-in-delay-2 {
  animation: fadeInUp 0.5s ease-out 0.2s forwards;
  opacity: 0;
}

.fade-in-delay-3 {
  animation: fadeInUp 0.5s ease-out 0.3s forwards;
  opacity: 0;
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
