<template>
  <div class="exam-task-container">
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

        <el-button
          type="primary"
          @click="handleSearch"
          style="margin-left: 15px"
        >
          <el-icon><Search /></el-icon>
          搜索
        </el-button>

        <el-button @click="handleReset">重置</el-button>
      </div>

      <div class="filter-right">
        <el-button type="primary" @click="handleCreateExam">
          <el-icon><Plus /></el-icon>
          新建考试
        </el-button>
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

      <el-table-column
        prop="paperName"
        label="关联试卷"
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
          <el-tag :type="getStatusType(getRealStatus(row))">
            {{ getStatusText(getRealStatus(row)) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="320" align="center" fixed="right">
        <template #default="{ row }">
          <!-- 未发布(0)：发布 / 编辑 / 删除 -->
          <template v-if="row.status === 0">
            <el-button
              link
              type="success"
              size="smal l"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>

          <!-- 未开始(2)：取消发布 -->
          <template v-else-if="getRealStatus(row) === 2">
            <el-button
              link
              type="info"
              size="small"
              @click="handleCancelPublish(row)"
            >
              取消发布
            </el-button>
          </template>

          <!-- 进行中(3)：结束考试 -->
          <template v-else-if="getRealStatus(row) === 3">
            <el-button
              link
              type="danger"
              size="small"
              @click="handleEndExam(row)"
            >
              结束考试
            </el-button>
          </template>

          <!-- 待批阅(4)：批阅（点击后状态→批阅完成(5)） -->
          <template v-else-if="getRealStatus(row) === 4">
            <el-button
              link
              type="warning"
              size="small"
              @click="handleReview(row)"
            >
              批阅
            </el-button>
          </template>

          <!-- 批阅完成(5)：删除考试 -->
          <template v-else-if="getRealStatus(row) === 5">
            <el-button
              link
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除考试
            </el-button>
          </template>
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
import { Search, Plus } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import {
  getExamPage,
  getAllExamStatus,
  updateExamStatus,
  publishExam,
  clearExamCache,
} from "@/api/examTask";

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

// 根据 startTime + duration 计算结束时间
const getEndTime = (row) => {
  const start = new Date(row.startTime);
  const end = new Date(start.getTime() + row.duration * 60 * 1000);
  const pad = (n) => String(n).padStart(2, "0");
  return `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(
    end.getDate()
  )} ${pad(end.getHours())}:${pad(end.getMinutes())}:${pad(end.getSeconds())}`;
};

// 计算真实状态：已发布(1)根据时间计算为 未开始(2)/进行中(3)/待批阅(4)
const getRealStatus = (row) => {
  if (row.status !== 1) return row.status;
  const now = new Date();
  const start = new Date(row.startTime);
  const end = new Date(start.getTime() + row.duration * 60 * 1000);
  if (now < start) return 2; // 未开始
  if (now < end) return 3; // 进行中
  return 4; // 待批阅
};

// 状态 → el-tag type
const getStatusType = (status) => {
  const map = {
    0: "info", // 未发布
    1: "warning", // 已发布
    2: "", // 未开始
    3: "success", // 进行中
    4: "warning", // 待批阅
    5: "success", // 批阅完成
  };
  return map[status] ?? "info";
};

// 状态 → 文本
const getStatusText = (status) => {
  const map = {
    0: "未发布",
    1: "已发布",
    2: "未开始",
    3: "进行中",
    4: "待批阅",
    5: "批阅完成",
  };
  return map[status] ?? "未知";
};

// ========== 状态下拉选项 ==========
const statusOptions = ref([]);

// 获取考试状态列表
const fetchStatusOptions = async () => {
  try {
    const res = await getAllExamStatus();
    const codes = res.data || [];
    statusOptions.value = codes.map((code) => ({
      code,
      label: getStatusText(code),
    }));
  } catch (error) {
    console.warn("获取考试状态列表失败:", error.message);
    // 降级：使用默认状态列表
    statusOptions.value = [
      { code: 0, label: "未发布" },
      { code: 1, label: "已发布" },
      { code: 2, label: "未开始" },
      { code: 3, label: "进行中" },
      { code: 4, label: "待批阅" },
      { code: 5, label: "批阅完成" },
    ];
  }
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
    const res = await getExamPage(params);
    pagination.total = res.data.total;
    tableData.value = res.data.records;
  } catch (error) {
    console.error("获取考试列表失败:", error);
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

// 新建考试 → 跳转到添加考试页面
const handleCreateExam = () => {
  router.push("/teacher/addExam");
};

// 编辑 → 跳转到添加考试页面（编辑模式）
const handleEdit = (row) => {
  router.push(`/teacher/addExam?examId=${row.id}`);
};

// 批阅 / 继续批阅（仅跳转，不改变状态）
const handleReview = (row) => {
  router.push({
    path: "/teacher/onlineGrade",
    query: { examId: row.id },
  });
};

// 查看成绩
const handleViewScore = (row) => {
  router.push(`/teacher/classExamScore?examId=${row.id}`);
};

// 发布考试
const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要发布考试"${row.examName}"吗？发布后学生将可以查看考试信息。`,
      "提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    await publishExam(row.id);
    await updateExamStatus(row.id, 1);
    ElMessage.success("发布成功");
    fetchExamList();
  } catch (error) {
    if (error !== "cancel") {
      console.error("发布失败:", error);
    }
  }
};

// 结束考试
const handleEndExam = async (row) => {
  try {
    await ElMessageBox.confirm(
      "您确定要结束考试吗？结束考试后，不能重新启用了",
      "提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    await updateExamStatus(row.id, 4);
    ElMessage.success("考试已结束，进入待批阅");
    fetchExamList();
  } catch (error) {
    if (error !== "cancel") {
      console.error("结束考试失败:", error);
    }
  }
};

// 取消发布
const handleCancelPublish = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消发布考试"${row.examName}"吗？取消后学生将无法查看该考试。`,
      "提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    await clearExamCache(row.id);
    await updateExamStatus(row.id, 0);
    ElMessage.success("已取消发布");
    fetchExamList();
  } catch (error) {
    if (error !== "cancel") {
      console.error("取消发布失败:", error);
    }
  }
};

// 公布成绩
const handlePublishScore = (row) => {
  router.push(`/teacher/classExamScore?examId=${row.id}`);
};

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除考试"${row.examName}"吗？`, "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      // TODO: 调用删除API
      ElMessage.success("删除成功");
      fetchExamList();
    })
    .catch(() => {
      ElMessage.info("已取消删除");
    });
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
  fetchStatusOptions();
  fetchExamList();
});
</script>

<style scoped>
.exam-task-container {
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

.filter-right {
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
