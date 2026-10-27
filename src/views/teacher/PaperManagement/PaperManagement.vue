<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Refresh, Plus, Delete } from "@element-plus/icons-vue";
import {
  getPaperPage,
  deletePapers,
  updatePaperStatus,
} from "@/api/paperManager";
import PaperDrawer from "./components/PaperDrawer.vue";

const router = useRouter();

// 搜索表单
const searchForm = reactive({
  name: "",
  paperStatus: "",
});

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

// 表格数据
const tableData = ref([]);
const loading = ref(false);
const selectedRows = ref([]);

// 抽屉相关
const drawerVisible = ref(false);
const currentEditData = ref(null);

// 模拟数据
const mockPapers = [
  {
    id: 1,
    name: "Java基础期末考试",
    totalScore: 100,
    description: "涵盖Java基础语法、面向对象、集合框架等核心知识点",
    creatorId: 1,
    creatorName: "张老师",
    createTime: "2025-06-01 09:00:00",
    updateTime: "2025-06-10 14:30:00",
  },
  {
    id: 2,
    name: "Spring Boot单元测试",
    totalScore: 80,
    description: "测试Spring Boot自动配置、IOC、AOP等核心概念",
    creatorId: 2,
    creatorName: "李老师",
    createTime: "2025-06-05 10:00:00",
    updateTime: "2025-06-12 16:00:00",
  },
  {
    id: 3,
    name: "数据库原理期中考试",
    totalScore: 120,
    description: "包含SQL语句、范式理论、事务与并发控制等内容",
    creatorId: 1,
    creatorName: "张老师",
    createTime: "2025-05-20 08:30:00",
    updateTime: "2025-06-01 11:00:00",
  },
  {
    id: 4,
    name: "前端Vue3综合测验",
    totalScore: 50,
    description: "Vue3组合式API、响应式原理、路由与状态管理",
    creatorId: 2,
    creatorName: "李老师",
    createTime: "2025-06-08 13:00:00",
    updateTime: "2025-06-15 09:45:00",
  },
  {
    id: 5,
    name: "计算机网络期末考试",
    totalScore: 100,
    description: "TCP/IP协议栈、HTTP/HTTPS、网络安全基础",
    creatorId: 1,
    creatorName: "张老师",
    createTime: "2025-06-10 14:00:00",
    updateTime: "2025-06-18 10:20:00",
  },
  {
    id: 6,
    name: "数据结构与算法测验",
    totalScore: 150,
    description: "线性表、树、图、排序与查找算法综合分析",
    creatorId: 2,
    creatorName: "李老师",
    createTime: "2025-05-25 09:30:00",
    updateTime: "2025-06-05 15:10:00",
  },
  {
    id: 7,
    name: "操作系统模拟测试",
    totalScore: 100,
    description: "进程管理、内存管理、文件系统与I/O管理",
    creatorId: 1,
    creatorName: "张老师",
    createTime: "2025-06-12 11:00:00",
    updateTime: "2025-06-20 08:00:00",
  },
  {
    id: 8,
    name: "软件工程综合考试",
    totalScore: 80,
    description: "需求分析、系统设计、软件测试与项目管理",
    creatorId: 2,
    creatorName: "李老师",
    createTime: "2025-06-15 16:00:00",
    updateTime: "2025-06-22 12:30:00",
  },
];

// 获取试卷列表
const fetchPaperList = async () => {
  loading.value = true;
  try {
    const res = await getPaperPage({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      name: searchForm.name,
      paperStatus: searchForm.paperStatus || undefined,
    });
    tableData.value = res.data.records;
    pagination.total = res.data.total;
  } catch (error) {
    console.warn("后端接口不可用，使用模拟数据:", error.message);
    let filtered = mockPapers;
    if (searchForm.name) {
      filtered = filtered.filter((p) => p.name.includes(searchForm.name));
    }
    pagination.total = filtered.length;
    const start = (pagination.currentPage - 1) * pagination.pageSize;
    const end = start + pagination.pageSize;
    tableData.value = filtered.slice(start, end);
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  pagination.currentPage = 1;
  fetchPaperList();
};

// 重置
const handleReset = () => {
  searchForm.name = "";
  searchForm.paperStatus = "";
  pagination.currentPage = 1;
  fetchPaperList();
};

// 分页大小改变
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  fetchPaperList();
};

// 当前页改变
const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  fetchPaperList();
};

// 多选
const handleSelectionChange = (rows) => {
  selectedRows.value = rows;
};

// 新增
const handleAdd = () => {
  currentEditData.value = null;
  drawerVisible.value = true;
};

// 编辑 - 跳转到试卷内容页
const handleEdit = (row) => {
  router.push(`/teacher/paper-content/${row.id}`);
};

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除试卷"${row.name.substring(0, 30)}..."吗？`,
    "警告",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }
  )
    .then(async () => {
      try {
        await deletePapers([row.id]);
        ElMessage.success("删除成功");
      } catch {
        tableData.value = tableData.value.filter((p) => p.id !== row.id);
        pagination.total--;
        ElMessage.success("删除成功");
      }
      fetchPaperList();
    })
    .catch(() => {
      ElMessage.info("已取消删除");
    });
};

// 更新试卷状态（启用/取消启用）
const handleUpdateStatus = (row, newStatus) => {
  const actionText = newStatus === 2 ? "启用" : "取消启用";
  ElMessageBox.confirm(`确定要${actionText}试卷"${row.name}"吗？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "info",
  })
    .then(async () => {
      try {
        await updatePaperStatus(row.id, newStatus);
        ElMessage.success(`${actionText}成功`);
        fetchPaperList();
      } catch (error) {
        console.error("状态更新失败:", error);
      }
    })
    .catch(() => {
      ElMessage.info("已取消操作");
    });
};

// 批量删除
const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning("请先选择要删除的试卷");
    return;
  }
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedRows.value.length} 份试卷吗？`,
    "警告",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }
  )
    .then(async () => {
      const ids = selectedRows.value.map((r) => r.id);
      try {
        await deletePapers(ids);
        ElMessage.success("批量删除成功");
        selectedRows.value = [];
      } catch {
        tableData.value = tableData.value.filter((p) => !ids.includes(p.id));
        pagination.total -= ids.length;
        selectedRows.value = [];
        ElMessage.success("批量删除成功");
      }
      fetchPaperList();
    })
    .catch(() => {
      ElMessage.info("已取消删除");
    });
};

// 抽屉操作成功回调
const handleDrawerSuccess = () => {
  fetchPaperList();
};

onMounted(() => {
  fetchPaperList();
});
</script>

<template>
  <div class="paper-management-container">
    <!-- 顶部搜索栏 -->
    <el-card class="search-card" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="试卷名称">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入试卷名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作区 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增试卷
      </el-button>
      <el-button
        type="danger"
        plain
        @click="handleBatchDelete"
        :disabled="selectedRows.length === 0"
      >
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="hover">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />

        <el-table-column
          prop="name"
          label="试卷名称"
          min-width="200"
          show-overflow-tooltip
        />

        <el-table-column
          prop="totalScore"
          label="总分"
          width="100"
          align="center"
        >
          <template #default="{ row }"> {{ row.totalScore }} 分 </template>
        </el-table-column>

        <el-table-column
          prop="description"
          label="描述"
          min-width="250"
          show-overflow-tooltip
        />

        <el-table-column
          prop="teacherName"
          label="创建人"
          width="100"
          align="center"
        />

        <el-table-column prop="createTime" label="创建时间" width="180" />

        <el-table-column prop="updateTime" label="更新时间" width="180" />

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'DRAFT'" type="info" effect="plain">
              草稿
            </el-tag>
            <el-tag
              v-else-if="row.status === 'PENDING_ENABLE'"
              type="warning"
              effect="plain"
            >
              待启用
            </el-tag>
            <el-tag
              v-else-if="row.status === 'ALREADY_ENABLE'"
              type="success"
              effect="plain"
            >
              已启用
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_ENABLE'"
              type="success"
              link
              @click="handleUpdateStatus(row, 'ALREADY_ENABLE')"
            >
              启用
            </el-button>
            <el-button
              v-if="row.status === 'ALREADY_ENABLE'"
              type="warning"
              link
              @click="handleUpdateStatus(row, 'PENDING_ENABLE')"
            >
              取消启用
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 底部分页 -->
      <div class="pagination-wrapper">
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
    </el-card>
  </div>

  <!-- 新增/编辑抽屉 -->
  <PaperDrawer
    v-model:visible="drawerVisible"
    :edit-data="currentEditData"
    @success="handleDrawerSuccess"
  />
</template>

<style scoped>
.paper-management-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.search-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.action-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.table-card {
  border-radius: 8px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #fafafa;
  color: #606266;
  font-weight: 600;
}
</style>
