<template>
  <div class="profile-container">
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">{{ subtitleText }}</p>
      </div>
    </el-card>

    <!-- 顶部信息区域 -->
    <div class="info-grid" :class="{ 'full-width': !isStudent }">
      <!-- 用户基本信息卡片 -->
      <el-card class="user-card" shadow="hover">
        <div class="user-info-flex">
          <el-avatar :size="80" :src="userAvatar" class="user-avatar" />
          <div class="user-details">
            <h2 class="user-name">
              {{ userInfo.realName || userInfo.username || "--" }}
              <el-tag :type="roleTagType" size="small" class="role-tag">
                {{ roleLabel }}
              </el-tag>
            </h2>
            <div class="detail-item">
              <el-icon><User /></el-icon>
              <span>{{ userInfo.username }}</span>
            </div>
            <div class="detail-item">
              <el-icon><Calendar /></el-icon>
              <span>欢迎回来</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 学生：统计数据卡片 -->
      <div v-if="isStudent" class="stats-grid">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-text">
              <p class="stat-label">总考试数</p>
              <p class="stat-value text-primary">{{ examList.length }}</p>
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
              <p class="stat-value text-success">{{ passedCount }}</p>
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
              <p class="stat-value text-purple">{{ averageScore }}</p>
            </div>
            <div class="stat-icon-bg bg-purple">
              <el-icon><Document /></el-icon>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-text">
              <p class="stat-label">最高分</p>
              <p class="stat-value text-orange">{{ highestScore }}</p>
            </div>
            <div class="stat-icon-bg bg-orange">
              <el-icon><Star /></el-icon>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 教师/管理员：角色概览卡片 -->
      <div v-else class="role-overview">
        <el-card class="overview-card" shadow="hover">
          <div class="overview-content">
            <el-icon :size="48" class="overview-icon"><Monitor /></el-icon>
            <div class="overview-text">
              <h3>{{ overviewTitle }}</h3>
              <p>{{ overviewDesc }}</p>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 学生：图表区域 -->
    <template v-if="isStudent">
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

      <!-- 学生：最近考试记录表格 -->
      <el-card class="table-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>最近考试记录</span>
          </div>
        </template>
        <el-table
          :data="scoreHistory"
          style="width: 100%"
          stripe
          v-loading="loading"
        >
          <el-table-column prop="examName" label="考试名称" min-width="120" />
          <el-table-column label="考试日期" width="120">
            <template #default="scope">
              {{ formatDate(scope.row.examTime) }}
            </template>
          </el-table-column>
          <el-table-column label="得分" width="100">
            <template #default="scope">
              <span :class="getScoreClass(scope.row)">
                {{ scope.row.examScore ?? "--" }}分
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row)" effect="light">
                {{ getStatusText(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from "vue";
import * as echarts from "echarts";
import {
  User,
  Calendar,
  Trophy,
  TrendCharts,
  Document,
  Star,
  Monitor,
} from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import { getAllExamResult } from "@/api/examTask";

const userStore = useUserStore();

// --- 用户类型判断 ---
const isStudent = computed(() => userInfo.value.userType === 1);
const isTeacher = computed(() => userInfo.value.userType === 2);

const roleLabel = computed(() => {
  const map = { 1: "学生", 2: "教师", 3: "管理员" };
  return map[userInfo.value.userType] || "用户";
});

const roleTagType = computed(() => {
  const map = { 1: "primary", 2: "success", 3: "warning" };
  return map[userInfo.value.userType] || "info";
});

const subtitleText = computed(() => {
  if (isStudent.value) return "查看您的学习进度和考试记录";
  if (isTeacher.value) return "教学管理概览";
  return "系统管理概览";
});

const overviewTitle = computed(() => {
  if (isTeacher.value) return "教师工作台";
  return "管理员控制台";
});

const overviewDesc = computed(() => {
  if (isTeacher.value)
    return "您可以在左侧菜单中管理题库、创建考试和批阅试卷。";
  return "您可以在左侧菜单中管理系统用户、角色和权限配置。";
});

// --- 数据定义 ---
const userInfo = computed(() => userStore.userInfo);
const userAvatar = computed(() => {
  const name = userInfo.value.realName || userInfo.value.username || "user";
  return `https://api.dicebear.com/7.x/avataaars/svg?seed=${name}`;
});

const examList = ref([]);
const loading = ref(false);

// 计算通过考试数（得分 >= 总分 60%）
const passedCount = computed(() => {
  return examList.value.filter((item) => {
    const score = item.examScore;
    const total = item.totalScore;
    if (score == null || !total) return false;
    return score >= total * 0.6;
  }).length;
});

// 计算平均分
const averageScore = computed(() => {
  const scoredExams = examList.value.filter(
    (item) => item.examScore !== null && item.examScore !== undefined
  );
  if (scoredExams.length === 0) return 0;
  const sum = scoredExams.reduce((acc, item) => acc + item.examScore, 0);
  return Math.round(sum / scoredExams.length);
});

// 计算最高分
const highestScore = computed(() => {
  const scoredExams = examList.value.filter(
    (item) => item.examScore !== null && item.examScore !== undefined
  );
  if (scoredExams.length === 0) return 0;
  return Math.max(...scoredExams.map((item) => item.examScore));
});

// 成绩历史（用于表格和图表）
const scoreHistory = computed(() => {
  return examList.value
    .filter((item) => item.examScore !== null && item.examScore !== undefined)
    .slice(0, 6);
});

// --- 图表引用 ---
const barChartRef = ref(null);
const lineChartRef = ref(null);

// --- 方法 ---
const getScoreClass = (row) => {
  const score = row.examScore;
  const total = row.totalScore;
  if (score == null || !total) return "";
  const rate = (score / total) * 100;
  if (rate >= 80) return "text-success";
  if (rate >= 60) return "text-warning";
  return "text-danger";
};

const getStatusType = (row) => {
  if (row.examScore == null) return "warning";
  const score = row.examScore;
  const total = row.totalScore;
  if (score >= total * 0.6) return "success";
  return "danger";
};

const getStatusText = (row) => {
  if (row.examScore == null) return "待批阅";
  const score = row.examScore;
  const total = row.totalScore;
  if (score >= total * 0.6) return "通过";
  return "未通过";
};

const formatDate = (dateStr) => {
  if (!dateStr) return "--";
  const date = new Date(dateStr);
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${month}-${day}`;
};

const initCharts = () => {
  const historyData = scoreHistory.value;
  if (historyData.length === 0) return;

  // 初始化柱状图
  if (barChartRef.value) {
    const myChart = echarts.init(barChartRef.value);
    const option = {
      tooltip: { trigger: "axis" },
      grid: { left: "3%", right: "4%", bottom: "3%", containLabel: true },
      xAxis: {
        type: "category",
        data: historyData.map((item) => {
          const name = item.examName || "";
          return name.length > 6 ? name.substring(0, 6) + "..." : name;
        }),
        axisLabel: { fontSize: 12 },
      },
      yAxis: { type: "value", min: 0, max: 100 },
      series: [
        {
          name: "分数",
          type: "bar",
          data: historyData.map((item) => item.examScore),
          itemStyle: { color: "#3b82f6", borderRadius: [4, 4, 0, 0] },
        },
      ],
    };
    myChart.setOption(option);
    window.addEventListener("resize", () => myChart.resize());
  }

  // 初始化折线图（成绩趋势）
  if (lineChartRef.value) {
    const myChart = echarts.init(lineChartRef.value);
    const sortedData = [...historyData].sort((a, b) => {
      return new Date(a.examTime) - new Date(b.examTime);
    });
    const option = {
      tooltip: { trigger: "axis" },
      grid: { left: "3%", right: "4%", bottom: "3%", containLabel: true },
      xAxis: {
        type: "category",
        data: sortedData.map((item) => formatDate(item.examTime)),
      },
      yAxis: { type: "value", min: 0, max: 100 },
      series: [
        {
          name: "分数",
          type: "line",
          smooth: true,
          data: sortedData.map((item) => item.examScore),
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
    window.addEventListener("resize", () => myChart.resize());
  }
};

// 加载数据（仅学生需要加载考试成绩）
const loadData = async () => {
  if (!isStudent.value) return;
  loading.value = true;
  try {
    const res = await getAllExamResult();
    examList.value = res.data || [];
    await nextTick();
    initCharts();
  } catch (error) {
    console.error("获取考试数据失败:", error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.profile-container {
  padding: 24px;
  background-color: #f9fafb;
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
  color: #111827;
  margin: 0 0 8px 0;
}
.page-subtitle {
  color: #4b5563;
  margin: 0;
}

/* Grid Layouts */
.info-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  margin-bottom: 32px;
}
.info-grid.full-width {
  grid-template-columns: 1fr;
}
@media (min-width: 1024px) {
  .info-grid {
    grid-template-columns: 1fr 2fr;
  }
  .info-grid.full-width {
    grid-template-columns: 1fr;
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
  background-color: #dbeafe;
}
.user-name {
  font-size: 20px;
  font-weight: bold;
  color: #111827;
  margin: 0 0 4px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.role-tag {
  vertical-align: middle;
}
.detail-item {
  display: flex;
  align-items: center;
  color: #6b7280;
  font-size: 14px;
  margin-top: 4px;
}
.detail-item .el-icon {
  margin-right: 4px;
  font-size: 16px;
}

/* Role Overview (teacher/admin) */
.role-overview {
  display: flex;
  align-items: stretch;
}
.overview-card {
  width: 100%;
}
.overview-content {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px 0;
}
.overview-icon {
  color: #3b82f6;
  flex-shrink: 0;
}
.overview-text h3 {
  font-size: 20px;
  font-weight: bold;
  color: #111827;
  margin: 0 0 8px 0;
}
.overview-text p {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
  line-height: 1.6;
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
