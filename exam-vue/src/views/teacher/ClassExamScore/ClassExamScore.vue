<template>
  <div class="class-exam-score-container">
    <!-- 顶部导航栏 -->
    <div class="nav-bar-section">
      <el-select
        v-model="currentExamId"
        placeholder="选择考试"
        :loading="examLoading"
        clearable
        style="width: 240px"
        @change="handleExamChange"
      >
        <el-option
          v-for="item in examOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </div>
    <!-- 顶部核心指标区 -->
    <el-row :gutter="20" class="top-cards-section">
      <el-col :xs="24" :sm="12" :md="8" :lg="4" :xl="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon blue">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">参考人数</div>
              <div class="stat-value">{{ statistics.totalStudents }}人</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="5" :xl="5">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon green">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">班级平均分</div>
              <div class="stat-value">{{ statistics.avgScore }}分</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="5" :xl="5">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon orange">
              <el-icon><Rank /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">最高分/最低分</div>
              <div class="stat-value">
                {{ statistics.highestScore }} / {{ statistics.lowestScore }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="5" :xl="5">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon purple">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">及格率</div>
              <div class="stat-value">{{ statistics.passRate }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="5" :xl="5">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon red">
              <el-icon><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">优秀率</div>
              <div class="stat-value">{{ statistics.excellentRate }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 中部图表分析区 -->
    <el-row :gutter="20" class="charts-section">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>分数段分布</span>
            </div>
          </template>
          <div ref="scoreDistributionChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
          <div class="card-header">
            <span>主客观平均得分</span>
          </div>
          </template>
          <div ref="questionTypeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部成绩明细表格 -->
    <el-card shadow="hover" class="table-section">
      <template #header>
        <div class="table-header">
          <span>成绩明细</span>
          <el-button type="primary" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出成绩单(Excel)
          </el-button>
        </div>
      </template>

      <el-table
        :data="paginatedData"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
        :default-sort="{ prop: 'totalScore', order: 'descending' }"
      >
        <el-table-column
          prop="rank"
          label="排名"
          width="80"
          align="center"
          sortable
        />

        <el-table-column
          prop="studentNo"
          label="学号"
          min-width="130"
          show-overflow-tooltip
        />

        <el-table-column
          prop="studentName"
          label="学生姓名"
          min-width="130"
          show-overflow-tooltip
        />

        <el-table-column
          prop="objectiveScore"
          label="客观题得分"
          min-width="130"
          align="center"
          sortable
        />

        <el-table-column
          prop="subjectiveScore"
          label="主观题得分"
          min-width="130"
          align="center"
          sortable
        />

        <el-table-column
          prop="totalScore"
          label="总分"
          min-width="110"
          align="center"
          sortable
        >
          <template #default="{ row }">
            <span class="total-score">{{ row.totalScore }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '已阅卷' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click="handleViewPaper(row)"
            >
              查看试卷
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from "vue";
import * as echarts from "echarts";
import { ElMessage } from "element-plus";
import {
  User,
  TrendCharts,
  Rank,
  CircleCheck,
  Star,
  Download,
} from "@element-plus/icons-vue";
import { useRouter, useRoute } from "vue-router";
import { getExamPage, getGradeStudentList } from "@/api/examTask";

//初始化路由
const router = useRouter();
const route = useRoute();

// 当前考试ID
const currentExamId = ref(route.query.examId || "");

// 考试下拉选项
const examOptions = ref([]);
const examLoading = ref(false);

// 加载考试列表
const loadExamOptions = async () => {
  examLoading.value = true;
  try {
    const res = await getExamPage({ current: 1, size: 100 });
    const records = res.data?.records || [];
    examOptions.value = records.map((e) => ({
      value: e.id,
      label: e.examName || `考试${e.id}`,
    }));
  } catch (e) {
    console.error("加载考试列表失败", e);
  } finally {
    examLoading.value = false;
  }
};

// 切换考试
const handleExamChange = (val) => {
  currentExamId.value = val;
  loadTableData();
};

// 统计数据（由成绩列表计算）
const statistics = computed(() => {
  const list = tableData.value;
  const total = list.length;
  if (total === 0) {
    return { totalStudents: 0, avgScore: 0, highestScore: 0, lowestScore: 0, passRate: 0, excellentRate: 0 };
  }
  const scores = list.map((s) => s.totalScore || 0);
  const sum = scores.reduce((a, b) => a + b, 0);
  const highest = Math.max(...scores);
  const lowest = Math.min(...scores);
  const pass = scores.filter((s) => s >= 60).length;
  const excellent = scores.filter((s) => s >= 90).length;
  return {
    totalStudents: total,
    avgScore: Number((sum / total).toFixed(1)),
    highestScore: highest,
    lowestScore: lowest,
    passRate: Number(((pass / total) * 100).toFixed(0)),
    excellentRate: Number(((excellent / total) * 100).toFixed(0)),
  };
});

// 分数段分布数据
const scoreDistribution = computed(() => {
  const buckets = { "<60": 0, "60-70": 0, "70-80": 0, "80-90": 0, ">90": 0 };
  tableData.value.forEach((s) => {
    const v = s.totalScore || 0;
    if (v < 60) buckets["<60"]++;
    else if (v < 70) buckets["60-70"]++;
    else if (v < 80) buckets["70-80"]++;
    else if (v < 90) buckets["80-90"]++;
    else buckets[">90"]++;
  });
  return Object.values(buckets);
});

// 主客观平均得分（雷达图）
const abilityRadar = computed(() => {
  const list = tableData.value;
  if (list.length === 0) return [0, 0];
  const objSum = list.reduce((a, s) => a + (s.objectiveScore || 0), 0);
  const subSum = list.reduce((a, s) => a + (s.subjectiveScore || 0), 0);
  return [
    Number((objSum / list.length).toFixed(1)),
    Number((subSum / list.length).toFixed(1)),
  ];
});

// 图表引用
const scoreDistributionChartRef = ref(null);
const questionTypeChartRef = ref(null);
let scoreChart = null;
let questionChart = null;

// 加载状态
const loading = ref(false);

// 表格数据
const tableData = ref([]);

// 分页信息
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

// 计算分页后的数据
const paginatedData = computed(() => {
  const start = (pagination.currentPage - 1) * pagination.pageSize;
  const end = start + pagination.pageSize;
  return tableData.value.slice(start, end);
});

// 表格数据由 loadTableData 从接口加载

// 初始化分数段分布柱状图
const initScoreDistributionChart = () => {
  if (!scoreDistributionChartRef.value) return;

  if (scoreChart) scoreChart.dispose();
  const chart = scoreChart = echarts.init(scoreDistributionChartRef.value);

  const option = {
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "shadow",
      },
    },
    grid: {
      left: "3%",
      right: "4%",
      bottom: "3%",
      containLabel: true,
    },
    xAxis: {
      type: "category",
      data: ["<60", "60-70", "70-80", "80-90", ">90"],
      axisLine: {
        lineStyle: {
          color: "#e4e7ed",
        },
      },
      axisLabel: {
        color: "#606266",
      },
    },
    yAxis: {
      type: "value",
      name: "人数",
      axisLine: {
        lineStyle: {
          color: "#e4e7ed",
        },
      },
      axisLabel: {
        color: "#606266",
      },
      splitLine: {
        lineStyle: {
          color: "#f2f6fc",
        },
      },
    },
    series: [
      {
        name: "人数",
        type: "bar",
        data: scoreDistribution.value,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "#409eff" },
            { offset: 1, color: "#79bbff" },
          ]),
          borderRadius: [4, 4, 0, 0],
        },
        barWidth: "40%",
      },
    ],
  };

  chart.setOption(option);

  // 响应式调整
  window.addEventListener("resize", () => {
    chart.resize();
  });
};

// 初始化题型得分率雷达图
const initQuestionTypeChart = () => {
  if (!questionTypeChartRef.value) return;

  const chart = echarts.init(questionTypeChartRef.value);

  const option = {
    tooltip: {
      trigger: "item",
    },
    radar: {
      indicator: [
        { name: "客观题", max: 100 },
        { name: "主观题", max: 100 },
      ],
      shape: "polygon",
      splitNumber: 5,
      axisName: {
        color: "#606266",
      },
      splitLine: {
        lineStyle: {
          color: "#e4e7ed",
        },
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: ["rgba(64, 158, 255, 0.05)", "rgba(64, 158, 255, 0.1)"],
        },
      },
      axisLine: {
        lineStyle: {
          color: "#e4e7ed",
        },
      },
    },
    series: [
      {
        name: "主客观平均得分",
        type: "radar",
        data: [
          {
            value: abilityRadar.value,
            name: "得分率",
            itemStyle: {
              color: "#409eff",
            },
            areaStyle: {
              color: "rgba(64, 158, 255, 0.3)",
            },
            lineStyle: {
              color: "#409eff",
              width: 2,
            },
          },
        ],
      },
    ],
  };

  chart.setOption(option);

  // 响应式调整
  window.addEventListener("resize", () => {
    chart.resize();
  });
};

// 加载表格数据
const loadTableData = async () => {
  if (!currentExamId.value) {
    tableData.value = [];
    pagination.total = 0;
    return;
  }
  loading.value = true;
  try {
    const res = await getGradeStudentList(currentExamId.value);
    const list = res.data || [];
    tableData.value = list.map((s, index) => ({
      id: s.studentId,
      rank: index + 1,
      studentNo: s.studentNo,
      studentName: s.studentName,
      objectiveScore: s.objectiveScore,
      subjectiveScore: s.subjectiveScore,
      totalScore: s.totalScore,
      status: s.status,
    }));
    pagination.total = tableData.value.length;
    await nextTick();
    drawCharts();
  } catch (e) {
    console.error("加载成绩数据失败", e);
    ElMessage.error("加载成绩数据失败");
  } finally {
    loading.value = false;
  }
};

// 导出成绩单
const handleExport = () => {
  ElMessage.success("正在导出成绩单...");
  // TODO: 实现Excel导出逻辑
  setTimeout(() => {
    ElMessage.success("导出成功");
  }, 1500);
};

// 查看试卷
const handleViewPaper = (row) => {
  ElMessage.info(`查看 ${row.studentName} 的试卷`);
  router.push({
    path: "/teacher/exam-detail",
    query: {
      studentId: row.id,
      examId: currentExamId.value,
    },
  });
};

// 分页大小改变
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  pagination.currentPage = 1;
};

// 当前页改变
const handleCurrentChange = (val) => {
  pagination.currentPage = val;
};

// 重绘两个图表
const drawCharts = () => {
  initScoreDistributionChart();
  initQuestionTypeChart();
};

// 初始化
onMounted(async () => {
  await loadExamOptions();
  if (currentExamId.value) {
    await loadTableData();
  }
  nextTick(() => {
    initScoreDistributionChart();
    initQuestionTypeChart();
  });
});
</script>

<style scoped>
.class-exam-score-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 顶部导航栏 */
.nav-bar-section {
  margin-bottom: 20px;
}

/* 顶部核心指标区 */
.top-cards-section {
  margin-bottom: 20px;
}

.stat-card {
  height: 100%;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-icon.orange {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.purple {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.red {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

/* 中部图表分析区 */
.charts-section {
  margin-bottom: 20px;
}

.chart-card {
  height: 100%;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.chart-container {
  width: 100%;
  height: 350px;
}

/* 底部成绩明细表格 */
.table-section {
  margin-top: 20px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.total-score {
  font-weight: bold;
  color: #409eff;
  font-size: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .class-exam-score-container {
    padding: 10px;
  }

  .stat-value {
    font-size: 20px;
  }

  .chart-container {
    height: 280px;
  }

  .table-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
}
</style>
