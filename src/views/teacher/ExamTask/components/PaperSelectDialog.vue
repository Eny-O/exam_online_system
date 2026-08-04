<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择试卷"
    width="850px"
    :close-on-click-modal="false"
    destroy-on-close
    class="paper-select-dialog"
  >
    <!-- 搜索栏 -->
    <div class="dialog-search-bar">
      <el-input
        v-model="searchName"
        placeholder="请输入试卷名称"
        clearable
        style="width: 220px; margin-right: 12px"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
      <el-button @click="handleReset">
        <el-icon><Refresh /></el-icon>
        重置
      </el-button>
    </div>

    <!-- 试卷表格 -->
    <el-table
      ref="tableRef"
      :data="tableData"
      v-loading="loading"
      stripe
      highlight-current-row
      style="width: 100%; margin-top: 16px"
      @current-change="handleCurrentChange"
      @row-click="handleRowClick"
    >
      <el-table-column
        prop="name"
        label="试卷名称"
        min-width="180"
        show-overflow-tooltip
      />

      <el-table-column
        prop="totalScore"
        label="总分"
        width="90"
        align="center"
      >
        <template #default="{ row }">
          <span class="score-text">{{ row.totalScore }} 分</span>
        </template>
      </el-table-column>

      <el-table-column
        prop="description"
        label="描述"
        min-width="200"
        show-overflow-tooltip
      />

      <el-table-column
        prop="creatorName"
        label="创建人"
        width="100"
        align="center"
      />

      <el-table-column
        prop="createTime"
        label="创建时间"
        width="170"
        align="center"
      />
    </el-table>

    <!-- 分页 -->
    <div class="dialog-pagination">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[5, 10, 20]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        small
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedPaper" @click="handleConfirm">
          确认选择
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import { Search, Refresh } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { getPaperPage } from "@/api/paperManager";

// 弹窗可见性（双向绑定）
const dialogVisible = defineModel({ type: Boolean, default: false });

// 事件
const emit = defineEmits(["confirm"]);

// 搜索
const searchName = ref("");

// 表格
const tableRef = ref(null);
const tableData = ref([]);
const loading = ref(false);
const selectedPaper = ref(null);

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 5,
  total: 0,
});

// 模拟试卷数据（后端不可用时降级）
const mockPapers = [
  { id: 1, name: "Java基础期末考试", totalScore: 100, description: "涵盖Java基础语法、面向对象、集合框架等核心知识点", creatorName: "张老师", createTime: "2025-06-01 09:00:00" },
  { id: 2, name: "Spring Boot单元测试", totalScore: 80, description: "测试Spring Boot自动配置、IOC、AOP等核心概念", creatorName: "李老师", createTime: "2025-06-05 10:00:00" },
  { id: 3, name: "数据库原理期中考试", totalScore: 120, description: "包含SQL语句、范式理论、事务与并发控制等内容", creatorName: "张老师", createTime: "2025-05-20 08:30:00" },
  { id: 4, name: "前端Vue3综合测验", totalScore: 50, description: "Vue3组合式API、响应式原理、路由与状态管理", creatorName: "李老师", createTime: "2025-06-08 13:00:00" },
  { id: 5, name: "计算机网络期末考试", totalScore: 100, description: "TCP/IP协议栈、HTTP/HTTPS、网络安全基础", creatorName: "张老师", createTime: "2025-06-10 14:00:00" },
  { id: 6, name: "数据结构与算法测验", totalScore: 150, description: "线性表、树、图、排序与查找算法综合分析", creatorName: "李老师", createTime: "2025-05-25 09:30:00" },
  { id: 7, name: "操作系统模拟测试", totalScore: 100, description: "进程管理、内存管理、文件系统与I/O管理", creatorName: "张老师", createTime: "2025-06-12 11:00:00" },
  { id: 8, name: "软件工程综合考试", totalScore: 80, description: "需求分析、系统设计、软件测试与项目管理", creatorName: "李老师", createTime: "2025-06-15 16:00:00" },
];

// 获取试卷列表
const fetchPaperList = async () => {
  loading.value = true;
  try {
    const res = await getPaperPage({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      name: searchName.value,
    });
    tableData.value = res.data.records;
    pagination.total = res.data.total;
  } catch (error) {
    console.warn("后端接口不可用，使用模拟数据:", error.message);
    let filtered = mockPapers;
    if (searchName.value) {
      filtered = filtered.filter((p) => p.name.includes(searchName.value));
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
  selectedPaper.value = null;
  fetchPaperList();
};

// 重置
const handleReset = () => {
  searchName.value = "";
  pagination.currentPage = 1;
  selectedPaper.value = null;
  fetchPaperList();
};

// 行点击选中
const handleRowClick = (row) => {
  selectedPaper.value = row;
  tableRef.value?.setCurrentRow(row);
};

// 当前行变化
const handleCurrentChange = (row) => {
  selectedPaper.value = row;
};

// 分页大小改变
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  pagination.currentPage = 1;
  fetchPaperList();
};

// 页码改变
const handlePageChange = (val) => {
  pagination.currentPage = val;
  fetchPaperList();
};

// 确认选择
const handleConfirm = () => {
  if (!selectedPaper.value) {
    ElMessage.warning("请先选择一份试卷");
    return;
  }
  emit("confirm", selectedPaper.value);
  dialogVisible.value = false;
  ElMessage.success(`已选择试卷：${selectedPaper.value.name}`);
};

// 弹窗打开时加载数据
watch(dialogVisible, (val) => {
  if (val) {
    // 重置状态
    searchName.value = "";
    selectedPaper.value = null;
    pagination.currentPage = 1;
    fetchPaperList();
  }
});
</script>

<style scoped>
.dialog-search-bar {
  display: flex;
  align-items: center;
}

.dialog-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.score-text {
  color: #409eff;
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 16px 20px;
}

:deep(.el-table__header-wrapper th) {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #303133;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
