<template>
  <div class="question-management-container">
    <!-- 顶部搜索栏 -->
    <el-card class="search-card" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="题目内容">
          <el-input
            v-model="searchForm.content"
            placeholder="请输入题目内容"
            clearable
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="题型">
          <el-select
            v-model="searchForm.type"
            placeholder="请选择题型"
            clearable
            style="width: 120px"
          >
            <el-option label="单选" value="single" />
            <el-option label="多选" value="multiple" />
            <el-option label="判断" value="judge" />
            <el-option label="简答" value="essay" />
          </el-select>
        </el-form-item>

        <el-form-item label="难度">
          <el-select
            v-model="searchForm.difficulty"
            placeholder="请选择难度"
            clearable
            style="width: 120px"
          >
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
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
      <el-button type="primary" plain @click="handleBatchImport">
        <el-icon><Upload /></el-icon>
        批量导入题目
      </el-button>
      <el-button type="primary" @click="handleAddQuestion">
        <el-icon><Plus /></el-icon>
        新增题目
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
          prop="content"
          label="题干"
          min-width="300"
          show-overflow-tooltip
        />

        <el-table-column prop="type" label="题型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">
              {{ getTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="knowledgePoint" label="所属知识点" width="150" />

        <el-table-column prop="difficulty" label="难度" width="100">
          <template #default="{ row }">
            <el-tag :type="getDifficultyTagType(row.difficulty)" effect="plain">
              {{ getDifficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="180" />

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">
              编辑
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
  <!-- 新增/编辑题目抽屉 -->
  <QuestionDrawer
    v-model:visible="drawerVisible"
    :edit-data="currentEditData"
    @success="handleDrawerSuccess"
  />

  <!-- 批量导入对话框 -->
  <el-dialog
    v-model="importDialogVisible"
    title="批量导入题目"
    width="500px"
    destroy-on-close
  >
    <el-upload
      drag
      :auto-upload="false"
      :limit="1"
      accept=".xlsx"
      :on-change="handleFileChange"
      :on-remove="handleFileRemove"
      :on-exceed="handleFileExceed"
    >
      <el-icon class="el-icon--upload"><Upload /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip">仅支持 .xlsx 格式文件</div>
      </template>
    </el-upload>

    <!-- 导入结果展示 -->
    <el-alert
      v-if="importResult"
      :type="importResult.failCount > 0 ? 'warning' : 'success'"
      :closable="false"
      style="margin-top: 16px"
    >
      成功导入 {{ importResult.successCount }} 道题目
      <span v-if="importResult.failCount > 0"
        >，{{ importResult.failCount }} 道失败</span
      >
      <div
        v-if="importResult.failMessages && importResult.failMessages.length"
        style="margin-top: 8px"
      >
        <div
          v-for="(msg, idx) in importResult.failMessages"
          :key="idx"
          style="line-height: 1.6"
        >
          {{ msg }}
        </div>
      </div>
    </el-alert>

    <template #footer>
      <el-button @click="handleDownloadTemplate">下载模板</el-button>
      <el-button @click="importDialogVisible = false">关闭</el-button>
      <el-button
        type="primary"
        :loading="importLoading"
        :disabled="!importFile"
        @click="handleConfirmImport"
      >
        确认导入
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { Search, Refresh, Upload, Plus, Delete } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import QuestionDrawer from "@/views/teacher/QuestionManagement/components/QuestionDrawer.vue";
import {
  getQuestionPage,
  getQuestionCompleteById,
  deleteQuestions,
  importQuestions,
  downloadQuestionTemplate,
} from "@/api/questionManager";

// 搜索表单
const searchForm = reactive({
  content: "",
  type: "",
  difficulty: "",
});

// 表格数据
const tableData = ref([]);
const loading = ref(false);

// 抽屉显示状态
const drawerVisible = ref(false);
const currentEditData = ref(null);

// 批量选中的行
const selectedRows = ref([]);

// 批量导入对话框
const importDialogVisible = ref(false);
const importFile = ref(null);
const importResult = ref(null);
const importLoading = ref(false);

// 分页参数
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

// 获取题目列表（分页 + 条件查询）
const fetchQuestionList = async () => {
  loading.value = true;
  try {
    const res = await getQuestionPage({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      content: searchForm.content,
      type: searchForm.type,
      difficulty: searchForm.difficulty,
    });
    tableData.value = res.data.records;
    pagination.total = res.data.total;
  } catch (error) {
    ElMessage.error(error.message || "获取题目列表失败");
  } finally {
    loading.value = false;
  }
};

// 题型标签类型
const getTypeTagType = (type) => {
  const typeMap = {
    single: "success",
    multiple: "warning",
    judge: "info",
    essay: "danger",
  };
  return typeMap[type] || "";
};

// 题型标签文本
const getTypeLabel = (type) => {
  const labelMap = {
    single: "单选",
    multiple: "多选",
    judge: "判断",
    essay: "简答",
  };
  return labelMap[type] || type;
};

// 难度标签类型
const getDifficultyTagType = (difficulty) => {
  const difficultyMap = {
    easy: "success",
    medium: "warning",
    hard: "danger",
  };
  return difficultyMap[difficulty] || "";
};

// 难度标签文本
const getDifficultyLabel = (difficulty) => {
  const labelMap = {
    easy: "简单",
    medium: "中等",
    hard: "困难",
  };
  return labelMap[difficulty] || difficulty;
};

// 查询
const handleSearch = () => {
  pagination.currentPage = 1;
  fetchQuestionList();
};

// 重置
const handleReset = () => {
  searchForm.content = "";
  searchForm.type = "";
  searchForm.difficulty = "";
  pagination.currentPage = 1;
  fetchQuestionList();
};

// 批量导入 - 打开对话框
const handleBatchImport = () => {
  importFile.value = null;
  importResult.value = null;
  importDialogVisible.value = true;
};

// 上传文件大小上限 5MB
const MAX_FILE_SIZE = 5 * 1024 * 1024;

// 文件选择变化
const handleFileChange = (file) => {
  if (file.raw.size > MAX_FILE_SIZE) {
    ElMessage.error("文件大小不能超过 5MB");
    importFile.value = null;
    return;
  }
  importFile.value = file.raw;
  importResult.value = null;
};

// 文件超出限制时的提示
const handleFileExceed = () => {
  ElMessage.warning("只能上传一个文件，请先移除已选文件");
};

// 文件移除时清除引用
const handleFileRemove = () => {
  importFile.value = null;
  importResult.value = null;
};

// 确认上传
const handleConfirmImport = async () => {
  if (!importFile.value) {
    ElMessage.warning("请先选择文件");
    return;
  }
  importLoading.value = true;
  try {
    const res = await importQuestions(importFile.value);
    importResult.value = res.data;
    if (res.data.failCount > 0) {
      ElMessage.warning(
        `导入完成，成功 ${res.data.successCount} 道，失败 ${res.data.failCount} 道`
      );
    } else {
      ElMessage.success(`导入成功 ${res.data.successCount} 道题目`);
      fetchQuestionList();
      importDialogVisible.value = false;
    }
  } catch (error) {
    ElMessage.error(error.message || "导入失败");
  } finally {
    importLoading.value = false;
  }
};

// 下载题目导入模板
const handleDownloadTemplate = async () => {
  try {
    const res = await downloadQuestionTemplate();
    const url = URL.createObjectURL(res.data);
    const a = document.createElement("a");
    a.href = url;
    a.download = "题目导入模板.xlsx";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  } catch (e) {
    ElMessage.error("下载模板失败");
  }
};

// 新增题目
const handleAddQuestion = () => {
  currentEditData.value = null;
  drawerVisible.value = true;
};

// 编辑 - 调用详情接口获取完整数据（含选项）后打开抽屉
const handleEdit = async (row) => {
  try {
    const res = await getQuestionCompleteById(row.id);
    currentEditData.value = res.data;
    drawerVisible.value = true;
  } catch (error) {
    console.error("获取题目详情失败:", error);
  }
};

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除题目"${row.content.substring(0, 30)}..."吗？`,
    "警告",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }
  )
    .then(async () => {
      try {
        await deleteQuestions([row.id]);
        ElMessage.success("删除成功");
        fetchQuestionList();
      } catch (error) {
        console.error("删除失败:", error);
      }
    })
    .catch(() => {
      ElMessage.info("已取消删除");
    });
};

// 表格选择变化
const handleSelectionChange = (rows) => {
  selectedRows.value = rows;
};

// 批量删除
const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning("请先选择要删除的题目");
    return;
  }
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedRows.value.length} 道题目吗？`,
    "警告",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }
  )
    .then(async () => {
      try {
        const ids = selectedRows.value.map((row) => row.id);
        await deleteQuestions(ids);
        ElMessage.success("批量删除成功");
        selectedRows.value = [];
        fetchQuestionList();
      } catch (error) {
        console.error("批量删除失败:", error);
      }
    })
    .catch(() => {
      ElMessage.info("已取消删除");
    });
};

// 分页大小改变
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  fetchQuestionList();
};

// 当前页改变
const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  fetchQuestionList();
};

// 组件挂载时获取数据
onMounted(() => {
  fetchQuestionList();
});

// 抽屉提交成功
const handleDrawerSuccess = () => {
  fetchQuestionList();
};
</script>

<style scoped>
.question-management-container {
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

:deep(.el-tag) {
  border-radius: 4px;
}
</style>
