<script setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { Search, Refresh, Delete, View, Close } from "@element-plus/icons-vue";
import {
  getQuestionPage,
  getQuestionCompleteById,
} from "@/api/questionManager";
import {
  questionBankTarget,
  questionBankResult,
} from "@/stores/questionBankState";

const emit = defineEmits(["confirm", "cancel"]);

// ===== 类型/难度选项 =====
const typeOptions = [
  { label: "单选题", value: "single" },
  { label: "多选题", value: "multiple" },
  { label: "判断题", value: "judge" },
  { label: "简答题", value: "essay" },
];
const difficultyOptions = [
  { label: "简单", value: "easy" },
  { label: "中等", value: "medium" },
  { label: "困难", value: "hard" },
];

// ===== 搜索 =====
const searchForm = reactive({
  content: "",
  type: "",
  difficulty: "",
  knowledgePoint: "",
});

// ===== 表格 =====
const tableData = ref([]);
const loading = ref(false);
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

// ===== 已选题目 =====
const selectedQuestions = ref([]);

// ===== 预览抽屉 =====
const previewVisible = ref(false);
const previewQuestion = ref(null);

// ===== 模拟数据 =====
const mockQuestions = [
  {
    id: 1,
    content: "Java中哪个关键字用于实现接口？",
    type: "single",
    difficulty: "easy",
    knowledgePoint: "Java基础",
    createTime: "2025-05-01 10:00:00",
    options: [
      { label: "A", content: "class" },
      { label: "B", content: "implements" },
      { label: "C", content: "extends" },
      { label: "D", content: "interface" },
    ],
    answer: "B",
  },
  {
    id: 2,
    content: "以下哪些是Collection接口的子接口？",
    type: "multiple",
    difficulty: "medium",
    knowledgePoint: "集合框架",
    createTime: "2025-05-02 11:00:00",
    options: [
      { label: "A", content: "List" },
      { label: "B", content: "Set" },
      { label: "C", content: "Map" },
      { label: "D", content: "Queue" },
    ],
    answer: "ABD",
  },
  {
    id: 3,
    content: "String对象一旦创建就不能被修改。",
    type: "judge",
    difficulty: "easy",
    knowledgePoint: "Java基础",
    createTime: "2025-05-03 09:00:00",
    options: [],
    answer: "true",
  },
  {
    id: 4,
    content: "请简述Spring Boot的自动配置原理。",
    type: "essay",
    difficulty: "hard",
    knowledgePoint: "Spring Boot",
    createTime: "2025-05-04 14:00:00",
    options: [],
    answer: "Spring Boot通过@EnableAutoConfiguration注解启用自动配置...",
  },
  {
    id: 5,
    content: "ArrayList和LinkedList的区别是什么？",
    type: "single",
    difficulty: "medium",
    knowledgePoint: "集合框架",
    createTime: "2025-05-05 08:30:00",
    options: [
      { label: "A", content: "ArrayList基于数组，LinkedList基于链表" },
      { label: "B", content: "ArrayList基于链表，LinkedList基于数组" },
      { label: "C", content: "两者没有区别" },
      { label: "D", content: "以上都不对" },
    ],
    answer: "A",
  },
  {
    id: 6,
    content: "以下哪些是Spring的常用注解？",
    type: "multiple",
    difficulty: "medium",
    knowledgePoint: "Spring",
    createTime: "2025-05-06 16:00:00",
    options: [
      { label: "A", content: "@Autowired" },
      { label: "B", content: "@Override" },
      { label: "C", content: "@Service" },
      { label: "D", content: "@Controller" },
    ],
    answer: "ACD",
  },
  {
    id: 7,
    content: "HTTP协议是无状态协议。",
    type: "judge",
    difficulty: "easy",
    knowledgePoint: "计算机网络",
    createTime: "2025-05-07 10:30:00",
    options: [],
    answer: "true",
  },
  {
    id: 8,
    content: "请解释什么是SQL注入攻击及防范措施。",
    type: "essay",
    difficulty: "hard",
    knowledgePoint: "数据库安全",
    createTime: "2025-05-08 13:00:00",
    options: [],
    answer: "SQL注入是通过在输入中插入SQL代码来攻击数据库...",
  },
  {
    id: 9,
    content: "MySQL中InnoDB存储引擎支持事务。",
    type: "judge",
    difficulty: "easy",
    knowledgePoint: "数据库",
    createTime: "2025-05-09 09:00:00",
    options: [],
    answer: "true",
  },
  {
    id: 10,
    content: "Vue3中哪个API用于创建响应式引用？",
    type: "single",
    difficulty: "easy",
    knowledgePoint: "Vue3",
    createTime: "2025-05-10 11:00:00",
    options: [
      { label: "A", content: "ref()" },
      { label: "B", content: "reactive()" },
      { label: "C", content: "computed()" },
      { label: "D", content: "watch()" },
    ],
    answer: "A",
  },
  {
    id: 11,
    content: "以下哪些属于Vue3的组合式API？",
    type: "multiple",
    difficulty: "medium",
    knowledgePoint: "Vue3",
    createTime: "2025-05-11 15:00:00",
    options: [
      { label: "A", content: "setup()" },
      { label: "B", content: "data()" },
      { label: "C", content: "onMounted()" },
      { label: "D", content: "computed()" },
    ],
    answer: "ACD",
  },
  {
    id: 12,
    content: "TCP三次握手的第二个步骤服务端发送什么标志位？",
    type: "single",
    difficulty: "medium",
    knowledgePoint: "计算机网络",
    createTime: "2025-05-12 10:00:00",
    options: [
      { label: "A", content: "SYN" },
      { label: "B", content: "ACK" },
      { label: "C", content: "SYN+ACK" },
      { label: "D", content: "FIN" },
    ],
    answer: "C",
  },
];

// ===== 获取题目列表 =====
const fetchQuestions = async () => {
  loading.value = true;
  try {
    const res = await getQuestionPage({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      content: searchForm.content,
      type: searchForm.type,
      difficulty: searchForm.difficulty,
      knowledgePoint: searchForm.knowledgePoint,
    });
    tableData.value = res.data.records;
    pagination.total = res.data.total;
  } catch (error) {
    console.warn("后端接口不可用，使用模拟数据");
    let filtered = [...mockQuestions];
    if (searchForm.content) {
      filtered = filtered.filter((q) => q.content.includes(searchForm.content));
    }
    if (searchForm.type) {
      filtered = filtered.filter((q) => q.type === searchForm.type);
    }
    if (searchForm.difficulty) {
      filtered = filtered.filter((q) => q.difficulty === searchForm.difficulty);
    }
    if (searchForm.knowledgePoint) {
      filtered = filtered.filter((q) =>
        q.knowledgePoint.includes(searchForm.knowledgePoint)
      );
    }
    pagination.total = filtered.length;
    const start = (pagination.currentPage - 1) * pagination.pageSize;
    tableData.value = filtered.slice(start, start + pagination.pageSize);
  } finally {
    loading.value = false;
  }
};

// ===== 搜索 / 重置 =====
const handleSearch = () => {
  pagination.currentPage = 1;
  fetchQuestions();
};

const handleReset = () => {
  searchForm.content = "";
  searchForm.type = "";
  searchForm.difficulty = "";
  searchForm.knowledgePoint = "";
  pagination.currentPage = 1;
  fetchQuestions();
};

// ===== 分页 =====
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  fetchQuestions();
};

const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  fetchQuestions();
};

// ===== 选题联动 =====
const isSelected = (id) => {
  return selectedQuestions.value.some((q) => q.id === id);
};

const toggleSelect = (row) => {
  if (isSelected(row.id)) {
    selectedQuestions.value = selectedQuestions.value.filter(
      (q) => q.id !== row.id
    );
  } else {
    selectedQuestions.value.push({ ...row });
  }
};

const removeSelected = (id) => {
  selectedQuestions.value = selectedQuestions.value.filter((q) => q.id !== id);
};

const clearSelected = () => {
  selectedQuestions.value = [];
};

// ===== 预览 =====
const handlePreview = async (question) => {
  try {
    const res = await getQuestionCompleteById(question.id);
    previewQuestion.value = res.data;
  } catch (error) {
    previewQuestion.value = question;
  }
  previewVisible.value = true;
};

// ===== 辅助函数 =====
const getTypeLabel = (type) => {
  const map = {
    single: "单选题",
    multiple: "多选题",
    judge: "判断题",
    essay: "简答题",
  };
  return map[type] || type;
};

const getTypeTagType = (type) => {
  const map = {
    single: "success",
    multiple: "warning",
    judge: "info",
    essay: "danger",
  };
  return map[type] || "";
};

const getDifficultyLabel = (d) => {
  const map = { easy: "简单", medium: "中等", hard: "困难" };
  return map[d] || d;
};

const getDifficultyTagType = (d) => {
  const map = { easy: "success", medium: "warning", hard: "danger" };
  return map[d] || "";
};

// ===== 确认 / 取消 =====
const handleConfirm = () => {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning("请至少选择一道题目");
    return;
  }
  // 写入共享响应式状态
  const target = questionBankTarget.value || {};
  questionBankResult.value = {
    groupId: target.groupId,
    questions: selectedQuestions.value,
  };
  questionBankTarget.value = null;
  emit("confirm");
};

const handleCancel = () => {
  questionBankTarget.value = null;
  emit("cancel");
};

// ===== 生命周期 =====
onMounted(() => {
  // 根据当前大题题型自动筛选
  const target = questionBankTarget.value;
  if (target && target.questionType) {
    searchForm.type = target.questionType;
  }
  fetchQuestions();
});
</script>

<template>
  <div class="question-bank-container">
    <!-- 顶部标题 -->
    <div class="page-header">
      <h2 class="page-title">题库选题</h2>
      <div class="header-actions">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleConfirm">
          确定 (已选 {{ selectedQuestions.length }} 题)
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="hover">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="题干">
          <el-input
            v-model="searchForm.content"
            placeholder="搜索题干内容"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="题型">
          <el-select
            v-model="searchForm.type"
            placeholder="全部题型"
            clearable
            style="width: 130px"
          >
            <el-option
              v-for="opt in typeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select
            v-model="searchForm.difficulty"
            placeholder="全部难度"
            clearable
            style="width: 130px"
          >
            <el-option
              v-for="opt in difficultyOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input
            v-model="searchForm.knowledgePoint"
            placeholder="搜索知识点"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            筛选
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 主体内容 -->
    <div class="main-content">
      <!-- 左侧题目表格 -->
      <div class="left-panel">
        <el-card class="table-card" shadow="hover">
          <el-table
            :data="tableData"
            v-loading="loading"
            stripe
            style="width: 100%; flex: 1"
            height="100%"
            @row-click="toggleSelect"
            :row-class-name="
              ({ row }) => (isSelected(row.id) ? 'selected-row' : '')
            "
          >
            <el-table-column width="50" align="center">
              <template #default="{ row }">
                <el-checkbox
                  :model-value="isSelected(row.id)"
                  @click.stop="toggleSelect(row)"
                />
              </template>
            </el-table-column>
            <el-table-column
              prop="content"
              label="题干"
              min-width="250"
              show-overflow-tooltip
            />
            <el-table-column
              prop="type"
              label="题型"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-tag :type="getTypeTagType(row.type)" size="small">
                  {{ getTypeLabel(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="difficulty"
              label="难度"
              width="90"
              align="center"
            >
              <template #default="{ row }">
                <el-tag
                  :type="getDifficultyTagType(row.difficulty)"
                  size="small"
                >
                  {{ getDifficultyLabel(row.difficulty) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="knowledgePoint"
              label="知识点"
              width="140"
              show-overflow-tooltip
            />
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="pagination.currentPage"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next"
              small
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </div>

      <!-- 右侧已选列表 -->
      <div class="right-panel">
        <el-card class="selected-card" shadow="hover">
          <template #header>
            <div class="selected-header">
              <span>已选题目 ({{ selectedQuestions.length }})</span>
              <el-button
                type="danger"
                size="small"
                text
                @click="clearSelected"
                :disabled="selectedQuestions.length === 0"
              >
                <el-icon><Delete /></el-icon>
                清空
              </el-button>
            </div>
          </template>
          <div class="selected-list">
            <div v-if="selectedQuestions.length === 0" class="empty-selected">
              请从左侧表格选择题目的
            </div>
            <div
              v-for="q in selectedQuestions"
              :key="q.id"
              class="selected-item"
            >
              <div class="selected-info">
                <el-tag :type="getTypeTagType(q.type)" size="small">
                  {{ getTypeLabel(q.type) }}
                </el-tag>
                <span class="selected-content">{{ q.content }}</span>
              </div>
              <div class="selected-actions">
                <el-button
                  type="primary"
                  size="small"
                  text
                  @click="handlePreview(q)"
                >
                  <el-icon><View /></el-icon>
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  text
                  @click="removeSelected(q.id)"
                >
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 题目预览抽屉 -->
    <el-drawer
      v-model="previewVisible"
      title="题目详情"
      size="500px"
      direction="rtl"
      :close-on-click-modal="true"
    >
      <div class="preview-content" v-if="previewQuestion">
        <div class="preview-section">
          <span class="preview-label">题型：</span>
          <el-tag :type="getTypeTagType(previewQuestion.type)" size="small">
            {{ getTypeLabel(previewQuestion.type) }}
          </el-tag>
        </div>
        <div class="preview-section">
          <span class="preview-label">难度：</span>
          <el-tag
            :type="getDifficultyTagType(previewQuestion.difficulty)"
            size="small"
          >
            {{ getDifficultyLabel(previewQuestion.difficulty) }}
          </el-tag>
        </div>
        <div class="preview-section" v-if="previewQuestion.knowledgePoint">
          <span class="preview-label">知识点：</span>
          <span>{{ previewQuestion.knowledgePoint }}</span>
        </div>
        <el-divider />
        <div class="preview-section">
          <span class="preview-label">题干：</span>
          <p class="preview-text">{{ previewQuestion.content }}</p>
        </div>
        <div
          class="preview-section"
          v-if="previewQuestion.options && previewQuestion.options.length > 0"
        >
          <span class="preview-label">选项：</span>
          <div class="preview-options">
            <div
              v-for="(opt, idx) in previewQuestion.options"
              :key="idx"
              class="preview-option"
            >
              {{ String.fromCharCode(65 + idx) }}. {{ opt.content || opt }}
            </div>
          </div>
        </div>
        <div class="preview-section">
          <span class="preview-label">参考答案：</span>
          <p class="preview-text answer-text">
            {{ previewQuestion.answer || "暂无" }}
          </p>
        </div>
        <div class="preview-section" v-if="previewQuestion.analysis">
          <span class="preview-label">解析：</span>
          <p class="preview-text">{{ previewQuestion.analysis }}</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.question-bank-container {
  display: flex;
  flex-direction: column;
  height: 75vh;
  background-color: #f5f7fa;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.page-title {
  margin: 0;
  font-size: 22px;
  color: #303133;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-card {
  margin-bottom: 16px;
  border-radius: 8px;
  flex-shrink: 0;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.main-content {
  display: flex;
  gap: 20px;
  flex: 1;
  overflow: hidden;
}

.left-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.right-panel {
  flex: 0 0 320px;
  max-width: 320px;
  overflow-y: auto;
}

.table-card {
  border-radius: 8px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 已选列表 */
.selected-card {
  border-radius: 8px;
  height: fit-content;
  position: sticky;
  top: 20px;
}

.selected-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.selected-list {
  max-height: 520px;
  overflow-y: auto;
}

.empty-selected {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}

.selected-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  gap: 8px;
}

.selected-item:last-child {
  border-bottom: none;
}

.selected-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.selected-content {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

/* 选中行高亮 */
:deep(.el-table .selected-row) {
  background-color: #ecf5ff !important;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #fafafa;
  color: #606266;
  font-weight: 600;
}

/* 预览抽屉 */
.preview-content {
  padding: 0 10px;
}

.preview-section {
  margin-bottom: 16px;
}

.preview-label {
  color: #909399;
  font-size: 14px;
  margin-right: 8px;
}

.preview-text {
  margin: 8px 0 0;
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
}

.answer-text {
  color: #67c23a;
  font-weight: 500;
}

.preview-options {
  margin-top: 8px;
  padding-left: 4px;
}

.preview-option {
  padding: 4px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}
</style>
