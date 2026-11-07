<template>
  <el-dialog
    v-model="dialogVisible"
    title="添加学生"
    width="900px"
    :close-on-click-modal="false"
    destroy-on-close
    class="student-select-dialog"
  >
    <!-- 搜索栏 -->
    <div class="dialog-search-bar">
      <el-input
        v-model="searchName"
        placeholder="学生姓名"
        clearable
        style="width: 180px; margin-right: 12px"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-input
        v-model="searchStudentNo"
        placeholder="学生学号"
        clearable
        style="width: 180px; margin-right: 12px"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="searchClassId"
        placeholder="选择班级"
        filterable
        clearable
        style="width: 180px; margin-right: 12px"
      >
        <el-option
          v-for="cls in classList"
          :key="cls.id"
          :label="cls.className"
          :value="cls.id"
        />
      </el-select>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
      <el-button @click="handleReset">
        <el-icon><Refresh /></el-icon>
        重置
      </el-button>
    </div>

    <!-- 已选提示 -->
    <div v-if="selectedStudents.length" class="selected-hint">
      已选择 <strong>{{ selectedStudents.length }}</strong> 名学生
      <el-button type="primary" link size="small" @click="clearSelection">
        清空选择
      </el-button>
    </div>

    <!-- 学生表格 -->
    <el-table
      ref="tableRef"
      :data="tableData"
      v-loading="loading"
      stripe
      style="width: 100%; margin-top: 12px"
      @selection-change="handleSelectionChange"
      :row-key="(row) => row.id"
    >
      <el-table-column
        type="selection"
        width="55"
        align="center"
        reserve-selection
      />
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column
        prop="studentNo"
        label="学号"
        width="130"
        show-overflow-tooltip
      />
      <el-table-column
        prop="realName"
        label="姓名"
        width="100"
        show-overflow-tooltip
      />
      <el-table-column
        prop="className"
        label="班级"
        min-width="150"
        show-overflow-tooltip
      />
      <el-table-column
        prop="major"
        label="专业"
        min-width="140"
        show-overflow-tooltip
      />
      <el-table-column
        prop="grade"
        label="年级"
        width="100"
        show-overflow-tooltip
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
        <el-button
          type="primary"
          :disabled="!selectedStudents.length"
          @click="handleConfirm"
        >
          确认添加
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import { Search, Refresh } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { getClassList, pageStudent } from "@/api/examTask";

// 弹窗可见性（双向绑定）
const dialogVisible = defineModel({ type: Boolean, default: false });

// 事件：返回选中的学生数组
const emit = defineEmits(["confirm"]);

// 班级列表（搜索条件用）
const classList = ref([]);

// 搜索
const searchName = ref("");
const searchStudentNo = ref("");
const searchClassId = ref(null);

// 表格
const tableRef = ref(null);
const tableData = ref([]);
const loading = ref(false);
const selectedStudents = ref([]);

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

// ========== 获取班级列表 ==========
const fetchClassList = async () => {
  try {
    const res = await getClassList();
    classList.value = res.data || [];
  } catch {
    classList.value = [
      { id: 1, className: "计算机2101班" },
      { id: 2, className: "计算机2102班" },
      { id: 3, className: "软件工程2102班" },
      { id: 4, className: "计算机科学2103班" },
      { id: 5, className: "信息管理2101班" },
      { id: 6, className: "网络工程2101班" },
      { id: 7, className: "大数据2101班" },
      { id: 8, className: "数字媒体2101班" },
    ];
  }
};

// ========== 获取学生列表 ==========
const fetchStudentList = async () => {
  loading.value = true;
  try {
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
    };
    if (searchName.value) params.realName = searchName.value;
    if (searchStudentNo.value) params.studentNo = searchStudentNo.value;
    if (searchClassId.value) params.classId = searchClassId.value;

    const res = await pageStudent(params);
    tableData.value = res.data.records;
    pagination.total = res.data.total;
  } catch (error) {
    console.warn("获取学生列表失败:", error.message);
    tableData.value = [];
    pagination.total = 0;
  } finally {
    loading.value = false;
  }
};

// ========== 搜索 / 重置 ==========
const handleSearch = () => {
  pagination.currentPage = 1;
  fetchStudentList();
};

const handleReset = () => {
  searchName.value = "";
  searchStudentNo.value = "";
  searchClassId.value = null;
  pagination.currentPage = 1;
  fetchStudentList();
};

// ========== 多选 ==========
const handleSelectionChange = (rows) => {
  selectedStudents.value = rows;
};

const clearSelection = () => {
  tableRef.value?.clearSelection();
  selectedStudents.value = [];
};

// ========== 分页 ==========
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  pagination.currentPage = 1;
  fetchStudentList();
};

const handlePageChange = (val) => {
  pagination.currentPage = val;
  fetchStudentList();
};

// ========== 确认 ==========
const handleConfirm = () => {
  if (!selectedStudents.value.length) {
    ElMessage.warning("请先选择学生");
    return;
  }
  emit("confirm", selectedStudents.value);
  dialogVisible.value = false;
  ElMessage.success(`已添加 ${selectedStudents.value.length} 名学生`);
};

// ========== 弹窗打开时加载数据 ==========
watch(dialogVisible, (val) => {
  if (val) {
    searchName.value = "";
    searchStudentNo.value = "";
    searchClassId.value = null;
    selectedStudents.value = [];
    pagination.currentPage = 1;
    fetchClassList();
    fetchStudentList();
  }
});
</script>

<style scoped>
.dialog-search-bar {
  display: flex;
  align-items: center;
}

.selected-hint {
  margin-top: 10px;
  padding: 6px 12px;
  background-color: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 4px;
  font-size: 13px;
  color: #67c23a;
  display: flex;
  align-items: center;
  gap: 6px;
}

.selected-hint strong {
  color: #67c23a;
  font-size: 15px;
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

:deep(.el-dialog__body) {
  padding: 16px 20px;
}

:deep(.el-table__header-wrapper th) {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #303133;
}
</style>
