<template>
  <div class="result-view-container">
    <div class="content-wrapper">
      <!-- 标题部分 -->
      <div class="header-section fade-in">
        <h1 class="page-title">成绩查询</h1>
        <p class="page-subtitle">查看您的所有考试成绩</p>
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

      <!-- 搜索栏 -->
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
          v-loading="loading"
        >
          <el-table-column prop="examName" label="考试名称" min-width="180">
            <template #default="scope">
              <span class="font-medium">{{ scope.row.examName }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="examTime" label="考试时间" width="160">
            <template #default="scope">
              <div class="date-cell">
                <Calendar class="date-icon" />
                <span>{{ scope.row.examTime ?? "--" }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="得分/总分" width="120" align="center">
            <template #default="scope">
              <span
                v-if="scope.row.studentExamStatus === 'absent'"
                class="text-muted"
                >--</span
              >
              <span v-else :class="getScoreClass(scope.row)">
                {{ scope.row.examScore ?? "--" }}/{{
                  scope.row.totalScore ?? "--"
                }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="得分率" width="100" align="center">
            <template #default="scope">
              <span
                v-if="scope.row.studentExamStatus === 'absent'"
                class="text-muted"
                >--</span
              >
              <span v-else :class="getScoreRateClass(scope.row)">
                {{ getScoreRate(scope.row) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="实际用时" width="120" align="center">
            <template #default="scope">
              <span
                v-if="scope.row.studentExamStatus === 'absent'"
                class="text-muted"
                >--</span
              >
              <span v-else>{{ getActualDuration(scope.row) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row)" effect="light" round>
                {{ getStatusText(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
            label="操作"
            width="100"
            align="center"
            fixed="right"
          >
            <template #default="scope">
              <el-button
                v-if="
                  scope.row.examScore !== null &&
                  scope.row.studentExamStatus !== 'absent'
                "
                link
                type="primary"
                size="small"
                @click="handleViewDetail(scope.row)"
              >
                详情
              </el-button>
              <span v-else class="text-muted">--</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 空状态 -->
        <div
          v-if="filteredResults.length === 0 && !loading"
          class="empty-state"
        >
          <Search class="empty-icon" />
          <p>没有找到匹配的成绩记录</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import {
  TrendCharts,
  Trophy,
  Document,
  Search,
  Calendar,
} from "@element-plus/icons-vue";
import { getAllExamResult } from "@/api/examTask";

const router = useRouter();

// 数据状态
const examList = ref([]);
const loading = ref(false);
const searchQuery = ref("");

// 计算属性
const filteredResults = computed(() => {
  if (!searchQuery.value) return examList.value;
  return examList.value.filter((item) =>
    item.examName?.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});

const averageScore = computed(() => {
  const scoredExams = examList.value.filter(
    (item) =>
      item.examScore !== null &&
      item.examScore !== undefined &&
      item.studentExamStatus !== "absent"
  );
  if (scoredExams.length === 0) return 0;
  const sum = scoredExams.reduce((acc, item) => acc + item.examScore, 0);
  return Math.round(sum / scoredExams.length);
});

const passedCount = computed(() => {
  return examList.value.filter((item) => {
    if (item.studentExamStatus === "absent") return false;
    const score = item.examScore;
    const total = item.totalScore;
    if (score == null || !total) return false;
    return score >= total * 0.6;
  }).length;
});

const totalCount = computed(() => examList.value.length);

const highestScore = computed(() => {
  const scoredExams = examList.value.filter(
    (item) =>
      item.examScore !== null &&
      item.examScore !== undefined &&
      item.studentExamStatus !== "absent"
  );
  if (scoredExams.length === 0) return 0;
  return Math.max(...scoredExams.map((item) => item.examScore));
});

// 方法
const getScoreClass = (row) => {
  const score = row.examScore;
  const total = row.totalScore;
  if (score == null || !total) return "";
  const rate = (score / total) * 100;
  if (rate >= 80) return "score-high";
  if (rate >= 60) return "score-medium";
  return "score-low";
};

const getScoreRate = (row) => {
  const score = row.examScore;
  const total = row.totalScore;
  if (score == null || !total) return "--";
  return Math.round((score / total) * 100) + "%";
};

const getScoreRateClass = (row) => {
  const score = row.examScore;
  const total = row.totalScore;
  if (score == null || !total) return "";
  const rate = (score / total) * 100;
  if (rate >= 80) return "score-high";
  if (rate >= 60) return "score-medium";
  return "score-low";
};

const getActualDuration = (row) => {
  const start = row.examTime;
  const end = row.submitTime;
  if (!start || !end) return "--";
  const diffMs = new Date(end) - new Date(start);
  if (diffMs <= 0) return "--";
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 60) return `${minutes}分钟`;
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  return mins > 0 ? `${hours}小时${mins}分钟` : `${hours}小时`;
};

const getStatusType = (row) => {
  if (row.studentExamStatus === "absent") return "danger";
  if (row.examScore == null) return "warning";
  const score = row.examScore;
  const total = row.totalScore;
  if (score >= total * 0.6) return "success";
  return "danger";
};

const getStatusText = (row) => {
  if (row.studentExamStatus === "absent") return "缺考";
  if (row.examScore == null) return "待批阅";
  const score = row.examScore;
  const total = row.totalScore;
  if (score >= total * 0.6) return "通过";
  return "未通过";
};

const handleViewDetail = (row) => {
  router.push(`/exam/${row.examId}/result`);
};

// 加载数据
const loadData = async () => {
  loading.value = true;
  try {
    const res = await getAllExamResult();
    examList.value = res.data || [];
  } catch (error) {
    console.error("获取成绩列表失败:", error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.result-view-container {
  min-height: 100vh;
  background-color: #f9fafb;
  padding: 24px;
}

.content-wrapper {
  max-width: 1152px;
  margin: 0 auto;
}

/* 标题样式 */
.header-section {
  margin-bottom: 32px;
}

.page-title {
  font-size: 30px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8px;
  margin-top: 0;
}

.page-subtitle {
  color: #4b5563;
  margin: 0;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

@media (min-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.stat-card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  padding: 24px;
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
  color: #6b7280;
  font-size: 14px;
  margin: 0 0 4px 0;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  margin: 0;
}

.stat-icon-bg {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon {
  width: 24px;
  height: 24px;
}

/* 主题颜色 */
.blue-theme .stat-value {
  color: #2563eb;
}
.blue-theme .stat-icon-bg {
  background-color: #dbeafe;
}
.blue-theme .stat-icon {
  color: #2563eb;
}

.green-theme .stat-value {
  color: #16a34a;
}
.green-theme .stat-icon-bg {
  background-color: #dcfce7;
}
.green-theme .stat-icon {
  color: #16a34a;
}

.purple-theme .stat-value {
  color: #9333ea;
}
.purple-theme .stat-icon-bg {
  background-color: #f3e8ff;
}
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

.search-input-wrapper {
  flex: 1;
  position: relative;
}

.custom-search-input :deep(.el-input__wrapper) {
  padding-left: 40px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
}

.custom-search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #d1d5db inset;
}

.custom-search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #3b82f6 inset !important;
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
  color: #16a34a;
}

.score-medium {
  font-weight: 700;
  color: #ca8a04;
}

.score-low {
  font-weight: 700;
  color: #dc2626;
}

.text-muted {
  color: #c0c4cc;
}

.empty-state {
  text-align: center;
  padding: 48px 0;
  color: #6b7280;
}

.empty-icon {
  width: 48px;
  height: 48px;
  color: #d1d5db;
  margin: 0 auto 16px auto;
  display: block;
}

/* 简单动画 */
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
