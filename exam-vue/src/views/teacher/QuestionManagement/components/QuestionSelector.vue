<template>
  <el-dialog
    v-model="visible"
    title="从题库挑选题目"
    width="900px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 顶部筛选区 -->
    <div class="filter-area">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="题型">
          <el-select
            v-model="searchForm.type"
            placeholder="全部题型"
            clearable
            style="width: 120px"
          >
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
            <el-option label="判断题" value="judge" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="输入题干关键词"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 中间表格区 -->
    <el-table
      ref="tableRef"
      :data="questionList"
      border
      stripe
      v-loading="loading"
      height="400"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column
        prop="content"
        label="题干"
        show-overflow-tooltip
        min-width="200"
      />
      <el-table-column prop="type" label="题型" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ getQuestionTypeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="difficulty"
        label="难度"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <el-tag :type="getDifficultyTag(row.difficulty)" size="small">
            {{ getDifficultyText(row.difficulty) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="分值" width="80" align="center">
        <template #default="{ row }">
          <span class="score-text">{{ row.score }}分</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 底部分页与统计区 -->
    <div class="dialog-footer-content">
      <div class="stats-info">
        <span
          >已选:
          <b class="highlight">{{ selectedQuestions.length }}</b> 题</span
        >
        <span class="divider">|</span>
        <span
          >总分: <b class="highlight">{{ totalSelectedScore }}</b> 分</span
        >
      </div>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        small
        @current-change="fetchQuestionList"
      />
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定选择</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from "vue";
import { ElMessage } from "element-plus";
import { getQuestionPage } from "@/api/questionManager";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  // 接收父组件已选的題目，用于回显勾选状态（可选）
  initialSelected: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(["update:modelValue", "confirm"]);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit("update:modelValue", val),
});

// 搜索表单
const searchForm = reactive({
  type: "",
  keyword: "",
});

// 分页配置
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
});

// 数据列表
const questionList = ref([]);
const selectedQuestions = ref([]);
const tableRef = ref(null);
const loading = ref(false);

// 模拟获取题库数据
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
    questionList.value = res.data.records;
    pagination.total = res.data.total;
  } catch (error) {
    ElMessage.error(error.message || "获取题目列表失败");
  } finally {
    loading.value = false;
  }
};

// Mock 数据
const mockData = Array.from({ length: pagination.pageSize }).map(
  (_, index) => ({
    id: (pagination.page - 1) * pagination.pageSize + index + 1,
    content: `这是一道关于 Vue3 组合式 API 的${
      index % 2 === 0 ? "单选" : "多选"
    }测试题目...`,
    type: index % 3 === 0 ? "single" : index % 3 === 1 ? "multiple" : "judge",
    difficulty: ["easy", "medium", "hard"][index % 3],
    score: index % 2 === 0 ? 5 : 10,
  })
);

questionList.value = mockData;
pagination.total = 50; // 假设总数

// 恢复勾选状态（如果有关联逻辑）
setTimeout(() => {
  if (tableRef.value && selectedQuestions.value.length > 0) {
    questionList.value.forEach((row) => {
      if (selectedQuestions.value.some((q) => q.id === row.id)) {
        tableRef.value.toggleRowSelection(row, true);
      }
    });
  }
}, 0);

// 搜索与重置
const handleSearch = () => {
  pagination.page = 1;
  fetchQuestionList();
};

const handleReset = () => {
  searchForm.type = "";
  searchForm.keyword = "";
  handleSearch();
};

// 表格选择变化
const handleSelectionChange = (val) => {
  selectedQuestions.value = val;
};

// 计算已选总分
const totalSelectedScore = computed(() => {
  return selectedQuestions.value.reduce((sum, item) => sum + item.score, 0);
});

// 辅助函数
const getQuestionTypeText = (type) => {
  const map = { single: "单选题", multiple: "多选题", judge: "判断题" };
  return map[type] || type;
};

const getDifficultyText = (diff) => {
  const map = { easy: "简单", medium: "中等", hard: "困难" };
  return map[diff] || diff;
};

const getDifficultyTag = (diff) => {
  const map = { easy: "success", medium: "warning", hard: "danger" };
  return map[diff] || "info";
};

// 确认选择
const handleConfirm = () => {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning("请至少选择一道题目");
    return;
  }
  emit("confirm", selectedQuestions.value);
  handleClose();
};

// 关闭弹窗
const handleClose = () => {
  visible.value = false;
  selectedQuestions.value = [];
  if (tableRef.value) tableRef.value.clearSelection();
};

// 监听弹窗打开，加载数据
watch(visible, (val) => {
  if (val) {
    fetchQuestionList();
  }
});
</script>

<style scoped>
.filter-area {
  margin-bottom: 15px;
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
}

.dialog-footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}

.stats-info {
  font-size: 14px;
  color: #606266;
}

.highlight {
  color: #409eff;
  font-weight: bold;
  font-size: 16px;
  margin: 0 4px;
}

.divider {
  margin: 0 10px;
  color: #dcdfe6;
}

.score-text {
  color: #67c23a;
  font-weight: 500;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
