<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Plus,
  Delete,
  Edit,
  Document,
  Select,
  Close,
  ArrowRight,
} from "@element-plus/icons-vue";
import {
  getPaperById,
  getPaperQuestions,
  savePaperQuestions,
} from "@/api/paperManager";
import {
  questionBankTarget,
  questionBankResult,
} from "@/stores/questionBankState";
import QuestionBankSelector from "@/views/teacher/QuestionManagement/QuestionBankSelector.vue";

const route = useRoute();
const router = useRouter();

// ===== 类型映射 =====
const typeMap = {
  single: "单选题",
  multiple: "多选题",
  judge: "判断题",
  essay: "简答题",
};
const typeList = ["single", "multiple", "judge", "essay"];

// ===== 页面状态 =====
const paperData = ref(null);
const loading = ref(false);
const questionList = ref([]); // [{id, type, score, questions:[{...}]}]
const selectedTypes = ref([]);
const activePanels = ref([]);
const nextGroupId = ref(1);
const nextQuestionSeq = ref(1);
const showQuestionBank = ref(false); // 题库选题弹窗

// ===== 题目勾选（批量删除用） =====
const checkedIds = ref(new Set());

const toggleCheck = (qId) => {
  const s = new Set(checkedIds.value);
  s.has(qId) ? s.delete(qId) : s.add(qId);
  checkedIds.value = s;
};

const isGroupAllChecked = (group) =>
  group.questions.length > 0 &&
  group.questions.every((q) => checkedIds.value.has(q.id));

const toggleGroupAll = (group) => {
  const s = new Set(checkedIds.value);
  if (isGroupAllChecked(group)) {
    group.questions.forEach((q) => s.delete(q.id));
  } else {
    group.questions.forEach((q) => s.add(q.id));
  }
  checkedIds.value = s;
};

const checkedCount = computed(() => checkedIds.value.size);

// ===== 统计 =====
const totalQuestions = computed(() =>
  questionList.value.reduce((s, g) => s + g.questions.length, 0)
);
const totalScore = computed(() =>
  questionList.value.reduce((s, g) => s + g.score * g.questions.length, 0)
);
const typeStats = computed(() => {
  return questionList.value.map((g) => ({
    type: g.type,
    label: typeMap[g.type],
    count: g.questions.length,
    scorePer: g.score,
    total: g.score * g.questions.length,
  }));
});

// ===== 获取试卷数据 =====
const fetchPaperData = async () => {
  loading.value = true;
  try {
    // 新增试卷：从路由 query 参数获取数据，不走后端
    if (route.params.id === "new") {
      paperData.value = {
        id: null,
        name: route.query.name || "",
        totalTime: Number(route.query.totalTime) || 120,
        description: route.query.description || "",
      };
      return;
    }

    const res = await getPaperById(route.params.id);
    paperData.value = res.data;
    // 尝试加载已有题目
    try {
      const qRes = await getPaperQuestions(route.params.id);
      if (qRes.data && qRes.data.length > 0) {
        // 按题型分组
        const groupMap = {};
        qRes.data.forEach((q) => {
          if (!groupMap[q.type]) {
            groupMap[q.type] = {
              id: nextGroupId.value++,
              type: q.type,
              score: q.score || 1,
              questions: [],
            };
          }
          groupMap[q.type].questions.push({
            ...q,
            id: q.id,
            seq: groupMap[q.type].questions.length + 1,
          });
        });
        questionList.value = Object.values(groupMap);
        activePanels.value = questionList.value.map((g) => g.id);
      }
    } catch {
      // 题目接口不可用时忽略
    }
  } catch (error) {
    console.warn("后端接口不可用，使用模拟数据");
    paperData.value = {
      id: route.params.id,
      name: "Java基础期末考试",
      totalTime: 120,
      description: "涵盖Java基础语法、面向对象、集合框架等核心知识点",
    };
  } finally {
    loading.value = false;
  }
};

// ===== 添加大题弹窗 =====
const addDialogVisible = ref(false);

const openAddDialog = () => {
  selectedTypes.value = [];
  dialogQuestionList.value = [];
  addDialogVisible.value = true;
};

const toggleType = (type) => {
  const idx = selectedTypes.value.indexOf(type);
  if (idx === -1) {
    selectedTypes.value.push(type);
    dialogQuestionList.value.push({
      id: Date.now() + Math.random(),
      type,
      seq: dialogQuestionList.value.length + 1,
      score: 1,
    });
  } else {
    selectedTypes.value = selectedTypes.value.filter((t) => t !== type);
    dialogQuestionList.value = dialogQuestionList.value.filter(
      (q) => q.type !== type
    );
    dialogQuestionList.value.forEach((q, i) => (q.seq = i + 1));
  }
};

const removeDialogItem = (id) => {
  const item = dialogQuestionList.value.find((q) => q.id === id);
  if (item) {
    selectedTypes.value = selectedTypes.value.filter((t) => t !== item.type);
  }
  dialogQuestionList.value = dialogQuestionList.value.filter(
    (q) => q.id !== id
  );
  dialogQuestionList.value.forEach((q, i) => (q.seq = i + 1));
};

const dialogQuestionList = ref([]);

const confirmAddType = () => {
  if (dialogQuestionList.value.length === 0) {
    ElMessage.warning("请至少选择一种题型");
    return;
  }
  dialogQuestionList.value.forEach((item) => {
    const existing = questionList.value.find((g) => g.type === item.type);
    if (existing) {
      existing.score = item.score;
    } else {
      questionList.value.push({
        id: nextGroupId.value++,
        type: item.type,
        score: item.score,
        questions: [],
      });
      activePanels.value.push(nextGroupId.value - 1);
    }
  });
  addDialogVisible.value = false;
  ElMessage.success("添加成功");
};

// ===== 删除大题 =====
const deleteQuestionType = (groupId) => {
  const group = questionList.value.find((g) => g.id === groupId);
  if (!group) return;
  ElMessageBox.confirm(
    `确定要删除"${typeMap[group.type]}"全部题目吗？`,
    "警告",
    { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" }
  )
    .then(() => {
      questionList.value = questionList.value.filter((g) => g.id !== groupId);
      activePanels.value = activePanels.value.filter((id) => id !== groupId);
      ElMessage.success("删除成功");
    })
    .catch(() => {});
};

// ===== 删除单题 =====
const deleteQuestion = (groupId, questionId) => {
  const group = questionList.value.find((g) => g.id === groupId);
  if (!group) return;
  group.questions = group.questions.filter((q) => q.id !== questionId);
  // 清理勾选状态
  const s = new Set(checkedIds.value);
  s.delete(questionId);
  checkedIds.value = s;
  // 如果分组为空则移除
  if (group.questions.length === 0) {
    questionList.value = questionList.value.filter((g) => g.id !== groupId);
    activePanels.value = activePanels.value.filter((id) => id !== groupId);
  }
  ElMessage.success("题目已删除");
};

// ===== 编辑大题分数 =====
const editTypeScore = (group) => {
  ElMessageBox.prompt(`请输入${typeMap[group.type]}每题分数`, "编辑每题分数", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    inputPattern: /^\d+(\.\d+)?$/,
    inputErrorMessage: "请输入有效分数",
    inputValue: group.score,
  })
    .then(({ value }) => {
      group.score = parseFloat(value);
      ElMessage.success("修改成功");
    })
    .catch(() => {});
};

// ===== 手动录入 =====
const manualEntry = (groupId) => {
  ElMessage.info("手动录入功能开发中");
  // TODO: 打开手动录入弹窗
};

// ===== 题库选题（覆盖层方式，组件不销毁，状态不丢失） =====
const goToQuestionBank = (groupId) => {
  const group = questionList.value.find((g) => g.id === groupId);
  if (!group) return;
  // 写入共享状态，告诉题库选择页要往哪个分组里加题
  questionBankTarget.value = {
    paperId: route.params.id,
    groupId: groupId,
    questionType: group.type,
  };
  showQuestionBank.value = true;
};

// 题库选题确认回调
const handleQuestionBankConfirm = () => {
  showQuestionBank.value = false;
  // 直接从共享状态读取结果
  const result = questionBankResult.value;
  if (!result) return;
  const { groupId, questions } = result;
  const group = questionList.value.find((g) => g.id === groupId);
  if (group && questions && questions.length > 0) {
    questions.forEach((q) => {
      if (!group.questions.some((eq) => eq.id === q.id)) {
        group.questions.push({
          ...q,
          id: q.id || Date.now() + Math.random(),
          seq: nextQuestionSeq.value++,
        });
      }
    });
    ElMessage.success(`已添加 ${questions.length} 道题目`);
  }
  questionBankResult.value = null;
  questionBankTarget.value = null;
};

// 题库选题取消回调
const handleQuestionBankCancel = () => {
  showQuestionBank.value = false;
  questionBankTarget.value = null;
  questionBankResult.value = null;
};

// ===== 批量删除（仅删除已勾选题目） =====
const batchDelete = () => {
  if (checkedIds.value.size === 0) {
    ElMessage.warning("请先勾选要删除的题目");
    return;
  }
  ElMessageBox.confirm(
    `确定要删除已勾选的 ${checkedIds.value.size} 道题目吗？`,
    "警告",
    { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" }
  )
    .then(() => {
      const ids = checkedIds.value;
      questionList.value.forEach((group) => {
        group.questions = group.questions.filter((q) => !ids.has(q.id));
      });
      // 移除空分组
      questionList.value = questionList.value.filter(
        (g) => g.questions.length > 0
      );
      checkedIds.value = new Set();
      ElMessage.success("删除成功");
    })
    .catch(() => {});
};

// ===== 完成 =====
const handleComplete = async () => {
  try {
    // 组装题目数据
    const questionsData = questionList.value.flatMap((group) =>
      group.questions.map((q, idx) => ({
        questionId: q.id,
        questionType: group.type,
        score: group.score,
        sortOrder: idx + 1,
      }))
    );
    await savePaperQuestions(route.params.id, { questions: questionsData });
    ElMessage.success("试卷保存成功");
  } catch (error) {
    console.warn("保存接口不可用:", error.message);
    ElMessage.info("已返回试卷管理页");
  }
  router.push("/teacher/exampaper");
};

// ===== 返回 =====
const handleBack = () => {
  router.push("/teacher/exampaper");
};

// ===== 获取类型标签样式 =====
const getTypeTagType = (type) => {
  const map = {
    single: "success",
    multiple: "warning",
    judge: "info",
    essay: "danger",
  };
  return map[type] || "";
};

// ===== 生命周期 =====
onMounted(async () => {
  await fetchPaperData();
});
</script>

<template>
  <div class="paper-content-container" v-loading="loading">
    <!-- 顶部标题栏 -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="handleBack" text>
          <el-icon><ArrowRight style="transform: rotate(180deg)" /></el-icon>
          返回
        </el-button>
        <h2 class="page-title">试卷内容</h2>
      </div>
      <el-button type="primary" size="large" @click="handleComplete">
        完成
      </el-button>
    </div>

    <!-- 试卷信息展示 -->
    <el-card class="info-card" shadow="hover" v-if="paperData">
      <div class="paper-info">
        <div class="info-item">
          <span class="info-label">试卷名称：</span>
          <span class="info-value">{{ paperData.name }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">答题时间：</span>
          <span class="info-value">{{ paperData.totalTime || 120 }} 分钟</span>
        </div>
        <div class="info-item">
          <span class="info-label">试卷说明：</span>
          <span class="info-value">{{
            paperData.description || "暂无说明"
          }}</span>
        </div>
      </div>
    </el-card>

    <!-- 主体内容 -->
    <div class="main-content">
      <!-- 左侧 80% -->
      <div class="left-panel">
        <div class="action-bar">
          <el-button type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon>
            添加大题
          </el-button>
          <el-button
            type="danger"
            plain
            @click="batchDelete"
            :disabled="checkedCount === 0"
          >
            <el-icon><Delete /></el-icon>
            批量删除{{ checkedCount > 0 ? ` (${checkedCount})` : "" }}
          </el-button>
        </div>

        <!-- 折叠面板 -->
        <el-collapse v-model="activePanels" class="question-collapse">
          <el-collapse-item
            v-for="group in questionList"
            :key="group.id"
            :name="group.id"
          >
            <template #title>
              <div class="collapse-header">
                <div class="header-left">
                  <el-checkbox
                    :model-value="isGroupAllChecked(group)"
                    @click.stop="toggleGroupAll(group)"
                    :disabled="group.questions.length === 0"
                    class="group-check"
                    >全选</el-checkbox
                  >
                  <el-button
                    type="primary"
                    size="small"
                    text
                    @click.stop="editTypeScore(group)"
                  >
                    <el-icon><Edit /></el-icon>
                    编辑
                  </el-button>
                  <el-button
                    type="warning"
                    size="small"
                    text
                    @click.stop="manualEntry(group.id)"
                  >
                    <el-icon><Document /></el-icon>
                    手动录入
                  </el-button>
                  <el-button
                    type="success"
                    size="small"
                    text
                    @click.stop="goToQuestionBank(group.id)"
                  >
                    <el-icon><Select /></el-icon>
                    题库选题
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    text
                    @click.stop="deleteQuestionType(group.id)"
                  >
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
                <div class="header-right">
                  <el-tag :type="getTypeTagType(group.type)" size="small">
                    {{ typeMap[group.type] }}
                  </el-tag>
                  <span class="type-seq">第 {{ group.id }} 大题</span>
                  <span class="type-name">{{ typeMap[group.type] }}</span>
                  <span class="score-info">
                    每题 <b>{{ group.score }}</b> 分，共
                    <b>{{ group.score * group.questions.length }}</b> 分
                  </span>
                </div>
              </div>
            </template>

            <!-- 题目列表 -->
            <div class="question-list">
              <div v-if="group.questions.length === 0" class="empty-tip">
                暂无题目，请通过"手动录入"或"题库选题"添加
              </div>
              <div
                v-for="(q, idx) in group.questions"
                :key="q.id"
                class="question-item"
                :class="{ 'is-checked': checkedIds.has(q.id) }"
              >
                <el-checkbox
                  :model-value="checkedIds.has(q.id)"
                  @change="toggleCheck(q.id)"
                  class="question-check"
                />
                <div class="question-main">
                  <div class="question-header">
                    <span class="q-seq">{{ idx + 1 }}.</span>
                    <span class="q-content">{{ q.content }}</span>
                  </div>
                  <div
                    class="q-options"
                    v-if="q.options && q.options.length > 0"
                  >
                    <div
                      v-for="(opt, oi) in q.options"
                      :key="oi"
                      class="q-option"
                    >
                      {{ String.fromCharCode(65 + oi) }}. {{ opt.content }}
                    </div>
                  </div>
                  <div class="q-answer">
                    <span class="answer-label">参考答案：</span>
                    <span>{{ q.answer || "暂无" }}</span>
                  </div>
                </div>
                <el-button
                  type="danger"
                  size="small"
                  text
                  @click="deleteQuestion(group.id, q.id)"
                  class="delete-btn"
                >
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>

        <div v-if="questionList.length === 0" class="empty-state">
          <el-empty description="暂无题目，点击上方「添加大题」按钮开始组卷" />
        </div>
      </div>

      <!-- 右侧 20% 统计面板 -->
      <div class="right-panel">
        <el-card class="stats-card" shadow="hover">
          <template #header>
            <span class="stats-title">试卷统计</span>
          </template>
          <div class="stats-content">
            <div class="stat-row total-row">
              <span>总题数</span>
              <span class="stat-value">{{ totalQuestions }} 题</span>
            </div>
            <div class="stat-row total-row">
              <span>总分</span>
              <span class="stat-value highlight">{{ totalScore }} 分</span>
            </div>
            <el-divider />
            <div class="stat-section">
              <div class="section-title">各题型统计</div>
              <div
                v-for="stat in typeStats"
                :key="stat.type"
                class="stat-row type-row"
              >
                <el-tag :type="getTypeTagType(stat.type)" size="small">
                  {{ stat.label }}
                </el-tag>
                <div class="type-detail">
                  <span
                    >共 {{ stat.count }} 题，每题 {{ stat.scorePer }} 分</span
                  >
                  <span class="type-total">共 {{ stat.total }} 分</span>
                </div>
              </div>
              <div v-if="typeStats.length === 0" class="no-data">
                暂无题型数据
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 添加大题弹窗 -->
    <el-dialog
      v-model="addDialogVisible"
      title="添加大题"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="add-dialog-content">
        <div class="type-buttons">
          <el-button
            v-for="type in typeList"
            :key="type"
            :type="selectedTypes.includes(type) ? 'primary' : 'default'"
            @click="toggleType(type)"
            class="type-btn"
          >
            {{ typeMap[type] }}
          </el-button>
        </div>
        <div class="type-preview" v-if="dialogQuestionList.length > 0">
          <div
            v-for="item in dialogQuestionList"
            :key="item.id"
            class="preview-item"
          >
            <span class="preview-seq">{{ item.seq }}</span>
            <span class="preview-type">{{ typeMap[item.type] }}</span>
            <div class="preview-score">
              每题分数：
              <el-input-number
                v-model="item.score"
                :min="0.5"
                :step="0.5"
                :precision="1"
                size="small"
                style="width: 120px"
              />
            </div>
            <el-button
              type="danger"
              size="small"
              text
              @click="removeDialogItem(item.id)"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 题库选题弹窗（el-dialog 不销毁当前页面组件，状态不丢失） -->
    <el-dialog
      v-model="showQuestionBank"
      title="题库选题"
      width="92%"
      top="4vh"
      :close-on-click-modal="false"
      class="question-bank-dialog"
      @close="handleQuestionBankCancel"
    >
      <QuestionBankSelector
        v-if="showQuestionBank"
        @confirm="handleQuestionBankConfirm"
        @cancel="handleQuestionBankCancel"
      />
    </el-dialog>
  </div>
</template>

<style scoped>
.paper-content-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  color: #303133;
  font-weight: 600;
}

.info-card {
  border-radius: 8px;
  margin-bottom: 20px;
}

.paper-info {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-label {
  color: #909399;
  font-size: 14px;
  white-space: nowrap;
}

.info-value {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.main-content {
  display: flex;
  gap: 20px;
}

.left-panel {
  flex: 0 0 80%;
  max-width: 80%;
}

.right-panel {
  flex: 0 0 calc(20% - 20px);
  max-width: calc(20% - 20px);
}

.action-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

/* 折叠面板 */
.question-collapse {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.collapse-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 10px;
}

.collapse-header .header-left {
  display: flex;
  gap: 2px;
}

.collapse-header .header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.type-seq {
  color: #606266;
  font-size: 13px;
}

.type-name {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.score-info {
  color: #909399;
  font-size: 13px;
}

.score-info b {
  color: #e6a23c;
}

/* 题目列表 */
.question-list {
  padding: 10px 15px;
}

.empty-tip {
  text-align: center;
  color: #c0c4cc;
  padding: 20px;
  font-size: 14px;
}

.question-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 8px;
  border-bottom: 1px dashed #ebeef5;
  border-radius: 4px;
  transition: background-color 0.15s;
}

.question-item:last-child {
  border-bottom: none;
}

.question-item.is-checked {
  background-color: #ecf5ff;
}

.question-check {
  margin-top: 2px;
  flex-shrink: 0;
}

.group-check {
  margin-right: 4px;
}

.question-main {
  flex: 1;
}

.question-header {
  display: flex;
  gap: 4px;
  margin-bottom: 6px;
}

.q-seq {
  color: #409eff;
  font-weight: 600;
  min-width: 24px;
}

.q-content {
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
}

.q-options {
  padding-left: 28px;
  margin-bottom: 6px;
}

.q-option {
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
}

.q-answer {
  padding-left: 28px;
  font-size: 13px;
  color: #67c23a;
}

.answer-label {
  color: #909399;
}

.delete-btn {
  flex-shrink: 0;
}

.empty-state {
  background: #fff;
  border-radius: 8px;
  padding: 60px 0;
}

/* 统计面板 */
.stats-card {
  border-radius: 8px;
  position: sticky;
  top: 20px;
}

.stats-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.stats-content {
  padding: 0;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.total-row {
  font-size: 15px;
  color: #303133;
}

.stat-value {
  font-weight: 600;
}

.stat-value.highlight {
  color: #e6a23c;
  font-size: 18px;
}

.stat-section {
  padding-top: 4px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.type-row {
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.type-row:last-child {
  border-bottom: none;
}

.type-detail {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 12px;
  color: #909399;
}

.type-total {
  color: #e6a23c;
  font-weight: 600;
  font-size: 13px;
}

.no-data {
  text-align: center;
  color: #c0c4cc;
  padding: 20px 0;
  font-size: 13px;
}

/* 添加大题弹窗 */
.add-dialog-content {
  padding: 0 10px;
}

.type-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 24px;
}

.type-btn {
  width: 100px;
  height: 40px;
  font-size: 14px;
}

.type-preview {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.preview-item:last-child {
  border-bottom: none;
}

.preview-seq {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.preview-type {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.preview-score {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

/* 折叠面板样式覆盖 */
:deep(.el-collapse-item__header) {
  padding: 0 15px;
  height: 50px;
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-collapse-item__wrap) {
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-collapse-item__content) {
  padding: 0;
}

:deep(.el-collapse) {
  border: 1px solid #ebeef5;
}
</style>

<!-- 题库选题弹窗样式（dialog 被 teleport 到 body，需要全局样式） -->
<style>
.question-bank-dialog .el-dialog__header {
  display: none;
}
.question-bank-dialog .el-dialog__body {
  padding: 0;
}
</style>
