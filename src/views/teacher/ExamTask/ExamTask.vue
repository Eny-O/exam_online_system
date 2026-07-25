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
          <el-option label="未发布" value="unpublished" />
          <el-option label="进行中" value="ongoing" />
          <el-option label="已结束" value="ended" />
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
        label="关联试卷名"
        min-width="150"
        show-overflow-tooltip
      />

      <el-table-column prop="className" label="参考班级" width="120" />

      <el-table-column label="考试时间段" min-width="200">
        <template #default="{ row }">
          <div>{{ row.startTime }} ~ {{ row.endTime }}</div>
        </template>
      </el-table-column>

      <el-table-column
        prop="duration"
        label="时长（分钟）"
        width="120"
        align="center"
      />

      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            size="small"
            @click="handleReview(row)"
          >
            批阅
          </el-button>
          <el-button link type="primary" size="small" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button
            link
            type="success"
            size="small"
            @click="handleViewScore(row)"
          >
            查看成绩
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">
            删除
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
  <!-- 新建/编辑考试抽屉 -->
  <el-drawer
    v-model="drawerVisible"
    :title="currentEditData ? '编辑考试' : '新建考试'"
    direction="rtl"
    size="800px"
    :before-close="handleDrawerClose"
  >
    <div class="drawer-content">
      <!-- 考试创建表单组件 -->
      <NewExamDrawer
        ref="examCreateFormRef"
        :edit-data="currentEditData"
        @success="handleCreateSuccess"
      />
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="handleDrawerClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit">{{
          currentEditData ? "保存" : "确定"
        }}</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { Search, Plus } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import NewExamDrawer from "./NewExamDrawer.vue";
import { useRouter } from "vue-router";

//初始化路由
const router = useRouter();

// 搜索表单
const searchForm = reactive({
  examName: "",
  status: "",
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

// 抽屉显示状态
const drawerVisible = ref(false);

// 当前编辑的数据
const currentEditData = ref(null);

// 表单引用
const examCreateFormRef = ref(null);

// Mock数据生成
const generateMockData = () => {
  const mockData = [
    {
      id: 1,
      examName: "2024年春季期末考试-Vue3基础",
      paperName: "Vue3基础知识试卷",
      className: "计算机2101班",
      startTime: "2024-06-15 09:00",
      endTime: "2024-06-15 11:00",
      duration: 120,
      status: "ongoing",
    },
    {
      id: 2,
      examName: "JavaScript高级程序设计期中测试",
      paperName: "JS高级概念测试卷",
      className: "软件工程2102班",
      startTime: "2024-05-20 14:00",
      endTime: "2024-05-20 16:00",
      duration: 120,
      status: "ended",
    },
    {
      id: 3,
      examName: "数据结构与算法单元测试",
      paperName: "树与图论专项测试",
      className: "计算机科学2103班",
      startTime: "2024-07-01 09:00",
      endTime: "2024-07-01 10:30",
      duration: 90,
      status: "unpublished",
    },
    {
      id: 4,
      examName: "数据库原理与应用期末考",
      paperName: "SQL查询优化试卷",
      className: "信息管理2101班",
      startTime: "2024-06-25 13:30",
      endTime: "2024-06-25 15:30",
      duration: 120,
      status: "ongoing",
    },
    {
      id: 5,
      examName: "操作系统原理阶段性测试",
      paperName: "进程管理与内存分配",
      className: "计算机2102班",
      startTime: "2024-05-10 09:00",
      endTime: "2024-05-10 10:30",
      duration: 90,
      status: "ended",
    },
    {
      id: 6,
      examName: "计算机网络基础测验",
      paperName: "TCP/IP协议栈测试",
      className: "网络工程2101班",
      startTime: "2024-07-05 14:00",
      endTime: "2024-07-05 15:30",
      duration: 90,
      status: "unpublished",
    },
    {
      id: 7,
      examName: "Python编程入门测试",
      paperName: "Python基础语法试卷",
      className: "大数据2101班",
      startTime: "2024-06-18 09:00",
      endTime: "2024-06-18 10:30",
      duration: 90,
      status: "ongoing",
    },
    {
      id: 8,
      examName: "Web前端开发综合考核",
      paperName: "HTML/CSS/JS综合测试",
      className: "数字媒体2101班",
      startTime: "2024-05-28 13:30",
      endTime: "2024-05-28 15:30",
      duration: 120,
      status: "ended",
    },
  ];

  return mockData;
};

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    unpublished: "info",
    ongoing: "primary",
    ended: "success",
  };
  return typeMap[status] || "info";
};

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    unpublished: "未发布",
    ongoing: "进行中",
    ended: "已结束",
  };
  return textMap[status] || "未知";
};

// 加载表格数据
const loadTableData = () => {
  loading.value = true;

  // 模拟API请求延迟
  setTimeout(() => {
    let filteredData = generateMockData();

    // 根据搜索条件过滤
    if (searchForm.examName) {
      filteredData = filteredData.filter((item) =>
        item.examName.includes(searchForm.examName)
      );
    }

    if (searchForm.status) {
      filteredData = filteredData.filter(
        (item) => item.status === searchForm.status
      );
    }

    // 更新总数
    pagination.total = filteredData.length;

    // 分页处理
    const start = (pagination.currentPage - 1) * pagination.pageSize;
    const end = start + pagination.pageSize;
    tableData.value = filteredData.slice(start, end);

    loading.value = false;
  }, 500);
};

// 搜索
const handleSearch = () => {
  pagination.currentPage = 1;
  loadTableData();
};

// 重置
const handleReset = () => {
  searchForm.examName = "";
  searchForm.status = "";
  pagination.currentPage = 1;
  loadTableData();
};

// 新建考试
const handleCreateExam = () => {
  currentEditData.value = null;
  drawerVisible.value = true;
};

// 关闭抽屉
const handleDrawerClose = () => {
  drawerVisible.value = false;
  currentEditData.value = null;
  if (examCreateFormRef.value) {
    examCreateFormRef.value.resetForm();
  }
};

// 提交表单
const handleSubmit = () => {
  if (examCreateFormRef.value) {
    examCreateFormRef.value.submit();
  }
};

// 创建成功回调
const handleCreateSuccess = () => {
  drawerVisible.value = false;
  ElMessage.success("考试创建成功");
  loadTableData();
};

//批阅
const handleReview = (row) => {
  ElMessage.info(`批阅考试：${row.examName}`);
  router.push({
    path: "/teacher/onlineGrade",
    query: {
      examId: row.id,
    },
  });
};

// 编辑
const handleEdit = (row) => {
  ElMessage.info(`编辑考试：${row.examName}`);
  // 模拟获取完整的考试详情数据
  const editData = getExamDetail(row.id);
  currentEditData.value = editData;
  drawerVisible.value = true;
};
// 模拟获取考试详情（实际应该调用API）
const getExamDetail = (id) => {
  // 这里模拟从表格数据中找到对应的完整信息
  const mockData = generateMockData();
  const exam = mockData.find((item) => item.id === id);

  if (!exam) return null;

  // 根据考试ID模拟不同的组卷策略和详细数据
  const isRandomStrategy = id % 2 === 0; // 偶数ID使用随机组卷

  return {
    id: exam.id,
    examName: exam.examName,
    paperId: exam.id, // 模拟关联试卷ID
    classIds: [exam.id], // 模拟班级ID数组
    startTime: exam.startTime,
    endTime: exam.endTime,
    duration: exam.duration,
    strategyType: isRandomStrategy ? "random" : "manual",
    // 手动组卷的题目配置
    manualQuestions: isRandomStrategy
      ? []
      : [
          {
            questionType: "single",
            count: 10,
            totalScore: 20,
          },
          {
            questionType: "multiple",
            count: 5,
            totalScore: 25,
          },
        ],
    // 随机组卷的规则
    randomRules: isRandomStrategy
      ? [
          {
            questionType: "single",
            count: 10,
            scorePerQuestion: 2,
            difficulty: "easy",
          },
          {
            questionType: "multiple",
            count: 5,
            scorePerQuestion: 5,
            difficulty: "medium",
          },
        ]
      : [],
  };
};
// 查看成绩
const handleViewScore = (row) => {
  ElMessage.info(`查看成绩：${row.examName}`);
  // TODO: 跳转到成绩页面
  router.push(`/teacher/classExamScore/${row.id}`);
};

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除考试"${row.examName}"吗？`, "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      ElMessage.success("删除成功");
      loadTableData();
    })
    .catch(() => {
      ElMessage.info("已取消删除");
    });
};

// 分页大小改变
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  pagination.currentPage = 1;
  loadTableData();
};

// 当前页改变
const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  loadTableData();
};

// 初始化
onMounted(() => {
  loadTableData();
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

/* 抽屉内容 */
.drawer-content {
  padding: 20px;
  height: calc(100vh - 120px);
  overflow-y: auto;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 10px 20px;
  border-top: 1px solid #e4e7ed;
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
