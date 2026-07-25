<template>
  <div class="profile-container">
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">查看您的学习进度和考试记录</p>
      </div>
    </el-card>

    <!-- 顶部信息区域 -->
    <div class="info-grid">
      <!-- 用户基本信息卡片 -->
      <el-card class="user-card" shadow="hover">
        <div class="user-info-flex">
          <el-avatar :size="80" :src="mockUser.avatar" class="user-avatar" />
          <div class="user-details">
            <h2 class="user-name">{{ mockUser.name }}</h2>
            <div class="detail-item">
              <el-icon><Message /></el-icon>
              <span>{{ mockUser.email }}</span>
            </div>
            <div class="detail-item">
              <el-icon><Calendar /></el-icon>
              <span>加入时间: {{ mockUser.joinDate }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 统计数据卡片 -->
      <div class="stats-grid">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-text">
              <p class="stat-label">总考试数</p>
              <p class="stat-value text-primary">{{ mockUser.totalExams }}</p>
            </div>
            <div class="stat-icon-bg bg-blue">
              <el-icon><Trophy /></el-icon>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-text">
              <p class="stat-label">通过考试</p>
              <p class="stat-value text-success">{{ mockUser.passedExams }}</p>
            </div>
            <div class="stat-icon-bg bg-green">
              <el-icon><TrendCharts /></el-icon>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-text">
              <p class="stat-label">平均分</p>
              <p class="stat-value text-purple">{{ mockUser.averageScore }}</p>
            </div>
            <div class="stat-icon-bg bg-purple">
              <el-icon><User /></el-icon>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-text">
              <p class="stat-label">学习时长</p>
              <p class="stat-value text-orange">{{ mockUser.totalTime }}</p>
            </div>
            <div class="stat-icon-bg bg-orange">
              <el-icon><Clock /></el-icon>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-grid">
      <el-card class="chart-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>考试成绩分布</span>
          </div>
        </template>
        <div ref="barChartRef" class="chart-container"></div>
      </el-card>

      <el-card class="chart-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>成绩趋势</span>
          </div>
        </template>
        <div ref="lineChartRef" class="chart-container"></div>
      </el-card>
    </div>

    <!-- 最近考试记录表格 -->
    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>最近考试记录</span>
        </div>
      </template>
      <el-table :data="scoreHistory" style="width: 100%" stripe>
        <el-table-column prop="name" label="考试名称" min-width="120" />
        <el-table-column prop="date" label="考试日期" width="120" />
        <el-table-column label="得分" width="100">
          <template #default="scope">
            <span :class="getScoreClass(scope.row.score)">
              {{ scope.row.score }}分
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag
              :type="scope.row.score >= 60 ? 'success' : 'danger'"
              effect="light"
            >
              {{ scope.row.score >= 60 ? "通过" : "未通过" }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from "vue";
import * as echarts from "echarts";
import {
  Message,
  Calendar,
  Trophy,
  TrendCharts,
  User,
  Clock,
} from "@element-plus/icons-vue";

// --- 数据定义 ---
const mockUser = ref({
  name: "张三",
  email: "zhangsan@example.com",
  avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan",
  joinDate: "2024-01-15",
  totalExams: 12,
  passedExams: 10,
  averageScore: 85,
  totalTime: "48小时",
});

const scoreHistory = ref([
  { name: "数学基础", score: 92, date: "2024-03-01" },
  { name: "英语听力", score: 78, date: "2024-03-05" },
  { name: "物理力学", score: 88, date: "2024-03-10" },
  { name: "化学实验", score: 95, date: "2024-03-15" },
  { name: "历史常识", score: 82, date: "2024-03-20" },
  { name: "编程基础", score: 90, date: "2024-03-25" },
]);

const trendData = ref([
  { month: "1月", score: 75 },
  { month: "2月", score: 78 },
  { month: "3月", score: 85 },
  { month: "4月", score: 82 },
  { month: "5月", score: 88 },
  { month: "6月", score: 90 },
]);

// --- 图表引用 ---
const barChartRef = ref(null);
const lineChartRef = ref(null);

// --- 方法 ---
const getScoreClass = (score) => {
  if (score >= 80) return "text-success";
  if (score >= 60) return "text-warning";
  return "text-danger";
};

const initCharts = () => {
  // 初始化柱状图
  if (barChartRef.value) {
    const myChart = echarts.init(barChartRef.value);
    const option = {
      tooltip: { trigger: "axis" },
      grid: { left: "3%", right: "4%", bottom: "3%", containLabel: true },
      xAxis: {
        type: "category",
        data: scoreHistory.value.map((item) => item.name),
        axisLabel: { fontSize: 12 },
      },
      yAxis: { type: "value", min: 0, max: 100 },
      series: [
        {
          name: "分数",
          type: "bar",
          data: scoreHistory.value.map((item) => item.score),
          itemStyle: { color: "#3b82f6", borderRadius: [4, 4, 0, 0] },
        },
      ],
    };
    myChart.setOption(option);

    // 响应式调整
    window.addEventListener("resize", () => myChart.resize());
  }

  // 初始化折线图
  if (lineChartRef.value) {
    const myChart = echarts.init(lineChartRef.value);
    const option = {
      tooltip: { trigger: "axis" },
      grid: { left: "3%", right: "4%", bottom: "3%", containLabel: true },
      xAxis: {
        type: "category",
        data: trendData.value.map((item) => item.month),
      },
      yAxis: { type: "value", min: 60, max: 100 },
      series: [
        {
          name: "分数",
          type: "line",
          smooth: true,
          data: trendData.value.map((item) => item.score),
          // 合并后的 itemStyle，包含线条颜色、符号边框及填充色
          itemStyle: {
            color: "#fff",
            borderColor: "#10b981",
            borderWidth: 2,
          },
          lineStyle: { color: "#10b981", width: 2 },
          symbol: "circle",
          symbolSize: 8,
        },
      ],
    };
    myChart.setOption(option);

    // 响应式调整
    window.addEventListener("resize", () => myChart.resize());
  }
};

onMounted(() => {
  nextTick(() => {
    initCharts();
  });
});
</script>

<style scoped>
.profile-container {
  padding: 24px;
  background-color: #f9fafb; /* bg-gray-50 */
  min-height: 100vh;
  max-width: 1200px;
  margin: 0 auto;
}

/* Header */
.header-card {
  margin-bottom: 32px;
  border: none;
  background: transparent;
}
.header-content {
  padding-left: 0;
}
.page-title {
  font-size: 30px;
  font-weight: bold;
  color: #111827; /* text-gray-900 */
  margin: 0 0 8px 0;
}
.page-subtitle {
  color: #4b5563; /* text-gray-600 */
  margin: 0;
}

/* Grid Layouts */
.info-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  margin-bottom: 32px;
}
@media (min-width: 1024px) {
  .info-grid {
    grid-template-columns: 1fr 2fr;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
@media (min-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}
@media (min-width: 1024px) {
  .chart-grid {
    grid-template-columns: 1fr 1fr;
  }
}

/* User Card */
.user-card {
  height: 100%;
}
.user-info-flex {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-avatar {
  background-color: #dbeafe; /* bg-blue-100 */
}
.user-name {
  font-size: 20px;
  font-weight: bold;
  color: #111827;
  margin: 0 0 4px 0;
}
.detail-item {
  display: flex;
  align-items: center;
  color: #6b7280; /* text-gray-500 */
  font-size: 14px;
  margin-top: 4px;
}
.detail-item .el-icon {
  margin-right: 4px;
  font-size: 16px;
}

/* Stat Cards */
.stat-card {
  padding: 16px;
}
.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stat-label {
  color: #6b7280;
  font-size: 14px;
  margin: 0 0 4px 0;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin: 0;
}
.stat-icon-bg {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.bg-blue {
  background-color: #dbeafe;
  color: #2563eb;
}
.bg-green {
  background-color: #dcfce7;
  color: #16a34a;
}
.bg-purple {
  background-color: #f3e8ff;
  color: #9333ea;
}
.bg-orange {
  background-color: #ffedd5;
  color: #ea580c;
}

.text-primary {
  color: #111827;
}
.text-success {
  color: #16a34a;
}
.text-purple {
  color: #9333ea;
}
.text-orange {
  color: #ea580c;
}
.text-warning {
  color: #d97706;
}
.text-danger {
  color: #dc2626;
}

/* Charts */
.chart-container {
  width: 100%;
  height: 250px;
}
.card-header {
  font-weight: bold;
  font-size: 18px;
  color: #111827;
}

/* Table overrides if needed */
.table-card {
  margin-top: 24px;
}
</style>
