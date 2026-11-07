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
      <el-select
        v-model="currentClassId"
        placeholder="全部班级"
        clearable
        :disabled="!currentExamId"
        style="width: 220px"
        @change="handleClassChange"
      >
        <el-option
          v-for="item in classOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <span class="nav-tip">清空班级即查看该考试全部成绩</span>
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
          <el-button
            type="primary"
            :loading="exporting"
            :disabled="!currentExamId || filteredData.length === 0"
            @click="handleExport"
          >
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
          prop="className"
          label="班级"
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

        <el-table-column
          prop="status"
          label="状态"
          min-width="110"
          align="center"
        >
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
          :total="filteredData.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from "vue";
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
import { getExamPage, getGradeStudentList, getClassList } from "@/api/examTask";
import { exportAoaToExcel } from "@/utils/exportExcel";

//初始化路由
const router = useRouter();
const route = useRoute();

// 当前考试ID
const currentExamId = ref(route.query.examId || "");

// 考试下拉选项
const examOptions = ref([]);
const examLoading = ref(false);

// 当前班级ID，空串表示不按班级过滤
const currentClassId = ref("");

// 班级ID到名称的映射
const classNameMap = ref({});

// 表格数据
const tableData = ref([]);

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

// 加载班级名称
const loadClassNameMap = async () => {
  try {
    const res = await getClassList();
    const list = Array.isArray(res.data) ? res.data : [];
    const map = {};
    list.forEach((c) => {
      if (c?.id != null) map[String(c.id)] = c.className || `班级 ${c.id}`;
    });
    classNameMap.value = map;
  } catch (e) {
    console.error("加载班级列表失败", e);
  }
};

// 班级名称
const classNameOf = (classId) => {
  if (classId == null) return "—";
  return classNameMap.value[String(classId)] || `班级 ${classId}`;
};

// 班级下拉选项
const classOptions = computed(() => {
  const ids = Array.from(
    new Set(tableData.value.map((s) => s.classId).filter((id) => id != null))
  );
  return ids.map((id) => ({ value: id, label: classNameOf(id) }));
});

// 按班级过滤后的成绩数据
const filteredData = computed(() => {
  const classId = currentClassId.value;
  if (classId === "" || classId == null) return tableData.value;
  return tableData.value.filter(
    (s) => s.classId != null && String(s.classId) === String(classId)
  );
});

// 切换考试
const handleExamChange = (val) => {
  currentExamId.value = val;
  currentClassId.value = "";
  pagination.currentPage = 1;
  loadTableData();
};

// 切换班级
const handleClassChange = async () => {
  pagination.currentPage = 1;
  await nextTick();
  drawCharts();
};

// 统计数据（由成绩列表计算）
const statistics = computed(() => {
  const list = filteredData.value;
  const total = list.length;
  if (total === 0) {
    return {
      totalStudents: 0,
      avgScore: 0,
      highestScore: 0,
      lowestScore: 0,
      passRate: 0,
      excellentRate: 0,
    };
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
  filteredData.value.forEach((s) => {
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
  const list = filteredData.value;
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

// 导出状态
const exporting = ref(false);

// 分页信息
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
});

// 计算分页后的数据
const paginatedData = computed(() => {
  const start = (pagination.currentPage - 1) * pagination.pageSize;
  const end = start + pagination.pageSize;
  return filteredData.value.slice(start, end);
});

// 表格数据由 loadTableData 从接口加载

// 初始化分数段分布柱状图
const initScoreDistributionChart = () => {
  if (!scoreDistributionChartRef.value) return;

  if (scoreChart) scoreChart.dispose();
  const chart = (scoreChart = echarts.init(scoreDistributionChartRef.value));

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
};

// 初始化题型得分率雷达图
const initQuestionTypeChart = () => {
  if (!questionTypeChartRef.value) return;

  if (questionChart) questionChart.dispose();
  const chart = (questionChart = echarts.init(questionTypeChartRef.value));

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
};

// 加载表格数据
const loadTableData = async () => {
  if (!currentExamId.value) {
    tableData.value = [];
    currentClassId.value = "";
    pagination.currentPage = 1;
    return;
  }
  loading.value = true;
  try {
    const res = await getGradeStudentList(currentExamId.value);
    const list = Array.isArray(res.data) ? res.data : [];
    const sorted = [...list].sort(
      (a, b) => (b.totalScore ?? 0) - (a.totalScore ?? 0)
    );
    tableData.value = sorted.map((s, index) => ({
      id: s.studentId,
      rank: index + 1,
      studentNo: s.studentNo,
      studentName: s.studentName,
      classId: s.classId ?? null,
      className: classNameOf(s.classId),
      objectiveScore: s.objectiveScore,
      subjectiveScore: s.subjectiveScore,
      totalScore: s.totalScore,
      status: s.status,
    }));
    pagination.currentPage = 1;
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
  if (exporting.value) return;
  if (!currentExamId.value || filteredData.value.length === 0) {
    ElMessage.warning("暂无可导出的成绩数据");
    return;
  }

  const examLabel =
    examOptions.value.find(
      (o) => String(o.value) === String(currentExamId.value)
    )?.label || `考试${currentExamId.value}`;
  const classLabel =
    currentClassId.value === "" || currentClassId.value == null
      ? "全部班级"
      : classOptions.value.find(
          (o) => String(o.value) === String(currentClassId.value)
        )?.label || `班级${currentClassId.value}`;

  const now = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  const dateText = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(
    now.getDate()
  )} ${pad(now.getHours())}:${pad(now.getMinutes())}`;
  const dateCode = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(
    now.getDate()
  )}`;

  const rows = filteredData.value.map((s) => [
    s.rank,
    s.studentNo ?? "",
    s.studentName ?? "",
    s.className ?? "—",
    s.objectiveScore ?? 0,
    s.subjectiveScore ?? 0,
    s.totalScore ?? 0,
    s.status ?? "",
  ]);

  const stats = statistics.value;
  const aoa = [
    [`${examLabel} 成绩单`],
    [`班级：${classLabel}`, `导出时间：${dateText}`],
    [],
    [
      "排名",
      "学号",
      "学生姓名",
      "班级",
      "客观题得分",
      "主观题得分",
      "总分",
      "状态",
    ],
    ...rows,
    [],
    ["统计摘要"],
    ["参考人数", `${stats.totalStudents}人`],
    ["平均分", `${stats.avgScore}分`],
    ["最高分", stats.highestScore],
    ["最低分", stats.lowestScore],
    ["及格率", `${stats.passRate}%`],
    ["优秀率", `${stats.excellentRate}%`],
  ];

  const safeName = (text) => String(text).replace(/[\\\/:*?"<>|]/g, "_");
  const fileName = `${safeName(examLabel)}_${safeName(
    classLabel
  )}_成绩单_${dateCode}.xlsx`;

  exporting.value = true;
  try {
    exportAoaToExcel(aoa, fileName, "成绩单", [8, 16, 12, 16, 14, 14, 10, 12]);
    ElMessage.success("导出成功");
  } catch (e) {
    console.error("导出成绩单失败", e);
    ElMessage.error("导出成绩单失败");
  } finally {
    exporting.value = false;
  }
};

// 查看试卷
const handleViewPaper = (row) => {
  const examId = currentExamId.value;
  if (!row?.id || !examId) {
    ElMessage.error("缺少学生或考试参数");
    return;
  }
  router.push({
    path: "/teacher/exam-detail",
    query: {
      studentId: row.id,
      examId,
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

// 窗口尺寸变化
const handleResize = () => {
  scoreChart?.resize();
  questionChart?.resize();
};

// 初始化
onMounted(async () => {
  window.addEventListener("resize", handleResize);
  await Promise.all([loadExamOptions(), loadClassNameMap()]);
  if (currentExamId.value) {
    await loadTableData();
  } else {
    await nextTick();
    drawCharts();
  }
});

onUnmounted(() => {
  window.removeEventListener("resize", handleResize);
  scoreChart?.dispose();
  questionChart?.dispose();
  scoreChart = null;
  questionChart = null;
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
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.nav-tip {
  font-size: 13px;
  color: #909399;
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
