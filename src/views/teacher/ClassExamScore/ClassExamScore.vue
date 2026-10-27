<template>
  <div class="class-exam-score-container">
    <!-- 顶部导航栏 -->
    <div class="nav-bar-section">
      <el-button type="primary" link @click="handleBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回试卷管理
      </el-button>
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
              <span>题型得分率</span>
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
          width="120"
          show-overflow-tooltip
        />

        <el-table-column
          prop="studentName"
          label="学生姓名"
          width="120"
          show-overflow-tooltip
        />

        <el-table-column
          prop="objectiveScore"
          label="客观题得分"
          width="120"
          align="center"
          sortable
        />

        <el-table-column
          prop="subjectiveScore"
          label="主观题得分"
          width="120"
          align="center"
          sortable
        />

        <el-table-column
          prop="totalScore"
          label="总分"
          width="100"
          align="center"
          sortable
        >
          <template #default="{ row }">
            <span class="total-score">{{ row.totalScore }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100" align="center">
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

//初始化路由
const router = useRouter();
const route = useRoute();

// 当前考试ID
const currentExamId = ref(route.query.examId || "");

// 统计数据
const statistics = reactive({
  totalStudents: 45,
  avgScore: 78.5,
  highestScore: 98,
  lowestScore: 52,
  passRate: 88,
  excellentRate: 30,
});

// 图表引用
const scoreDistributionChartRef = ref(null);
const questionTypeChartRef = ref(null);

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

// 生成模拟表格数据
const generateMockTableData = () => {
  const students = [
    { studentNo: "2021001", studentName: "张三" },
    { studentNo: "2021002", studentName: "李四" },
    { studentNo: "2021003", studentName: "王五" },
    { studentNo: "2021004", studentName: "赵六" },
    { studentNo: "2021005", studentName: "孙七" },
    { studentNo: "2021006", studentName: "周八" },
    { studentNo: "2021007", studentName: "吴九" },
    { studentNo: "2021008", studentName: "郑十" },
    { studentNo: "2021009", studentName: "陈十一" },
    { studentNo: "2021010", studentName: "林十二" },
    { studentNo: "2021011", studentName: "黄十三" },
    { studentNo: "2021012", studentName: "刘十四" },
    { studentNo: "2021013", studentName: "杨十五" },
    { studentNo: "2021014", studentName: "何十六" },
    { studentNo: "2021015", studentName: "马十七" },
  ];

  const data = students.map((student, index) => {
    const objectiveScore = Math.floor(Math.random() * 30) + 50;
    const subjectiveScore = Math.floor(Math.random() * 30) + 40;
    const totalScore = objectiveScore + subjectiveScore;
    const isAbsent = index === 3 || index === 8;

    return {
      rank: isAbsent ? "-" : index + 1,
      studentNo: student.studentNo,
      studentName: student.studentName,
      objectiveScore: isAbsent ? 0 : objectiveScore,
      subjectiveScore: isAbsent ? 0 : subjectiveScore,
      totalScore: isAbsent ? 0 : totalScore,
      status: isAbsent ? "缺考" : "已阅卷",
    };
  });

  // 按总分降序排序（排除缺考）
  data.sort((a, b) => {
    if (a.status === "缺考") return 1;
    if (b.status === "缺考") return -1;
    return b.totalScore - a.totalScore;
  });

  // 重新计算排名
  let rank = 1;
  data.forEach((item) => {
    if (item.status !== "缺考") {
      item.rank = rank++;
    }
  });

  return data;
};

// 初始化分数段分布柱状图
const initScoreDistributionChart = () => {
  if (!scoreDistributionChartRef.value) return;

  const chart = echarts.init(scoreDistributionChartRef.value);

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
        data: [5, 8, 15, 12, 5],
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
        { name: "单选题", max: 100 },
        { name: "多选题", max: 100 },
        { name: "判断题", max: 100 },
        { name: "简答题", max: 100 },
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
        name: "题型得分率",
        type: "radar",
        data: [
          {
            value: [85, 72, 90, 68],
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
const loadTableData = () => {
  loading.value = true;

  setTimeout(() => {
    tableData.value = generateMockTableData();
    pagination.total = tableData.value.length;
    loading.value = false;
  }, 500);
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

// 返回试卷管理
const handleBack = () => {
  router.back();
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

// 初始化
onMounted(() => {
  loadTableData();

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

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 16px;
}

.back-btn:hover {
  color: #409eff;
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
