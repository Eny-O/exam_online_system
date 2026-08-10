<template>
  <div class="exam-list-container">
    <!-- 顶部筛选区 -->
    <div class="filter-section">
      <div class="filter-left">
        <el-input
          v-model="searchForm.examName"
          placeholder="请输入考试名称"
          clearable
          style="width: 200px; margin-right: 15px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="searchForm.status"
          placeholder="考试状态"
          clearable
          style="width: 150px"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.code"
            :label="item.label"
            :value="item.code"
          />
        </el-select>

        <el-button type="primary" @click="handleSearch" style="margin-left: 15px">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>

        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 数据表格区 -->
    <el-table
      :data="tableData"
      v-loading="loading"
      border
      stripe
      style="width: 100%; margin-top: 20px"
    >
      <el-table-column
        prop="examName"
        label="考试名称"
        min-width="180"
        show-overflow-tooltip
      />

      <el-table-column label="考试时间段" min-width="200">
        <template #default="{ row }">
          <div>{{ row.startTime }} ~ {{ getEndTime(row) }}</div>
        </template>
      </el-table-column>

      <el-table-column
        prop="duration"
        label="时长（分钟）"
        width="120"
        align="center"
      />

      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <!-- 进行中(3)：进入考试 -->
          <el-button
            v-if="row.status === 3"
            link
            type="success"
            size="small"
            @click="handleEnterExam(row)"
          >
            进入考试
          </el-button>
          <!-- 未开始(2)：查看详情 -->
          <el-button
            v-if="row.status === 2"
            link
            type="primary"
            size="small"
            @click="handleExamDetail(row)"
          >
            查看详情
          </el-button>
          <!-- 已结束(4) / 待批阅(5) / 批阅完成(6)：查看成绩 -->
          <el-button
            v-if="row.status >= 4"
            link
            type="warning"
            size="small"
            @click="handleViewResult(row)"
          >
            查看成绩
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 底部分页区 -->
    <div class="pagination-section">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { Search } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { getStuExamPage } from "@/api/examTask";

const router = useRouter();

// 搜索表单
const searchForm = reactive({
  examName: "",
  status: undefined,
});

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

// ========== 状态相关 ==========

// 学生可见的状态选项（排除 0-未发布、1-已发布）
const statusOptions = [
  { code: 2, label: "未开始" },
  { code: 3, label: "进行中" },
  { code: 4, label: "已结束" },
  { code: 5, label: "待批阅" },
  { code: 6, label: "批阅完成" },
];

// 计算结束时间
const getEndTime = (row) => {
  const start = new Date(row.startTime);
  const end = new Date(start.getTime() + row.duration * 60 * 1000);
  const pad = (n) => String(n).padStart(2, "0");
  return `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())} ${pad(end.getHours())}:${pad(end.getMinutes())}:${pad(end.getSeconds())}`;
};

// 状态 → el-tag type
const getStatusType = (status) => {
  const map = {
    2: "",
    3: "success",
    4: "info",
    5: "warning",
    6: "success",
  };
  return map[status] ?? "info";
};

// 状态 → 文本
const getStatusText = (status) => {
  const map = {
    2: "未开始",
    3: "进行中",
    4: "已结束",
    5: "待批阅",
    6: "批阅完成",
  };
  return map[status] ?? "未知";
};

// ========== 数据加载 ==========

const fetchExamList = async () => {
  loading.value = true;
  try {
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
    };
    if (searchForm.examName) params.examName = searchForm.examName;
    if (
      searchForm.status !== undefined &&
      searchForm.status !== null &&
      searchForm.status !== ""
    ) {
      params.status = searchForm.status;
    }
    const res = await getStuExamPage(params);
    pagination.total = res.data.total;
    tableData.value = res.data.records;
  } catch (error) {
    console.error("获取考试列表失败:", error);
    ElMessage.error("获取考试列表失败");
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  pagination.currentPage = 1;
  fetchExamList();
};

// 重置
const handleReset = () => {
  searchForm.examName = "";
  searchForm.status = undefined;
  pagination.currentPage = 1;
  fetchExamList();
};

// ========== 操作 ==========

// 进入考试（进行中）
const handleEnterExam = (row) => {
  router.push(`/exam/${row.id}/take`);
};

// 查看详情（未开始）
const handleExamDetail = (row) => {
  router.push(`/exam/detail/${row.id}`);
};

// 查看成绩（已结束/待批阅/批阅完成）
const handleViewResult = (row) => {
  router.push(`/exam/${row.id}/result`);
};

// 分页大小改变
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  pagination.currentPage = 1;
  fetchExamList();
};

// 当前页改变
const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  fetchExamList();
};

// 初始化
onMounted(() => {
  fetchExamList();
});
</script>

<style scoped>
.exam-list-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 顶部筛选区 */
.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.filter-left {
  display: flex;
  align-items: center;
}

/* 表格区域 */
:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

:deep(.el-table__header-wrapper th) {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #303133;
}

/* 分页区域 */
.pagination-section {
  display: flex;
  justify-content: flex-end;
  padding: 20px;
  background-color: #ffffff;
  border-radius: 8px;
  margin-top: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .filter-section {
    flex-direction: column;
    gap: 15px;
  }

  .filter-left {
    width: 100%;
    flex-wrap: wrap;
    gap: 10px;
  }

  .filter-left .el-input,
  .filter-left .el-select {
    flex: 1;
    min-width: 150px;
  }
}
</style>
