<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from "vue";
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
  addPaper,
  updatePaper,
  getPaperById,
  savePaperQuestions,
} from "@/api/paperManager";
import {
  getQuestionCompleteById,
  addQuestionComplete,
} from "@/api/questionManager";
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
const hasUnsavedChanges = ref(false); // 是否有未保存的编辑

// 页面关闭/刷新前的离开提示
const beforeUnloadHandler = (e) => {
  if (hasUnsavedChanges.value) {
    e.preventDefault();
  }
};

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

// 全局题号映射：questionId -> 全局序号（跨分组连续编号）
const globalQuestionSeq = computed(() => {
  const map = {};
  let seq = 1;
  questionList.value.forEach((group) => {
    group.questions.forEach((q) => {
      map[q.id] = seq++;
    });
  });
  return map;
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
        description: route.query.description || "",
      };
      return;
    }

    // 编辑已有试卷：调用 queryPaperById，返回 PaperContentVO（含题目、选项、答案）
    const res = await getPaperById(route.params.id);
    const raw = res.data;
    paperData.value = {
      id: raw.id,
      name: raw.name,
      description: raw.description || "",
    };

    // 从 PaperContentVO.paperQuestionVOS 中加载题目（按题型分组）
    // API 返回结构: paperQuestionVOS: [{ sortOrder, score, questionCompleteVO: { id, content, type, ... } }]
    const paperQuestions = raw.paperQuestionVOS;
    if (paperQuestions && paperQuestions.length > 0) {
      const groupMap = {};
      // 按 sortOrder 排序确保题目顺序正确
      paperQuestions
        .slice()
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .forEach((pq) => {
          const q = pq.questionCompleteVO;
          if (!q) return;
          if (!groupMap[q.type]) {
            groupMap[q.type] = {
              id: nextGroupId.value++,
              type: q.type,
              score: pq.score || 1, // 从 paperQuestion 包装对象取每题分数
              questions: [],
            };
          }
          groupMap[q.type].questions.push({
            id: q.id,
            content: q.content,
            type: q.type,
            options: q.options || [],
            answer: q.answer || "",
            analysis: q.analysis || "",
            knowledgePoint: q.knowledgePoint || "",
            difficulty: q.difficulty || "",
          });
        });
      questionList.value = Object.values(groupMap);
      activePanels.value = questionList.value.map((g) => g.id);
    }
  } catch (error) {
    console.warn("后端接口不可用，使用模拟数据");
    paperData.value = {
      id: route.params.id,
      name: "Java基础期末考试",
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
  hasUnsavedChanges.value = true;
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
      hasUnsavedChanges.value = true;
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
  hasUnsavedChanges.value = true;
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
      hasUnsavedChanges.value = true;
      ElMessage.success("修改成功");
    })
    .catch(() => {});
};

// ===== 手动录入抽屉 =====
const manualDrawerVisible = ref(false);
const manualGroupId = ref(null);
const manualFormRef = ref(null);
const manualSubmitting = ref(false);

const manualForm = reactive({
  content: "",
  difficulty: "medium",
  knowledgePoint: "",
  options: [],
  answer: "",
  analysis: "",
});

const manualRules = {
  content: [
    { required: true, message: "请输入题目内容", trigger: "blur" },
    { min: 5, message: "题目内容至少5个字符", trigger: "blur" },
  ],
  answer: [{ required: true, message: "请输入正确答案", trigger: "blur" }],
};

// 当前手动录入的目标分组
const manualTargetGroup = computed(() =>
  questionList.value.find((g) => g.id === manualGroupId.value)
);

// 初始化选项（默认 A/B/C/D）
const manualInitOptions = () => {
  manualForm.options = [
    { label: "A", content: "" },
    { label: "B", content: "" },
    { label: "C", content: "" },
    { label: "D", content: "" },
  ];
  manualForm.answer = "";
};

// 添加选项
const manualAddOption = () => {
  const labels = ["A", "B", "C", "D", "E", "F", "G", "H"];
  const nextLabel = labels[manualForm.options.length];
  if (nextLabel) {
    manualForm.options.push({ label: nextLabel, content: "" });
  } else {
    ElMessage.warning("最多只能添加8个选项");
  }
};

// 删除选项
const manualDeleteOption = (index) => {
  if (manualForm.options.length <= 2) {
    ElMessage.warning("至少保留2个选项");
    return;
  }
  manualForm.options.splice(index, 1);
  const labels = ["A", "B", "C", "D", "E", "F", "G", "H"];
  manualForm.options.forEach((opt, idx) => {
    opt.label = labels[idx];
  });
};

// 多选答案转换：字符串 -> 数组
const manualGetMultipleAnswerArray = () => {
  if (manualTargetGroup.value?.type === "multiple" && manualForm.answer) {
    return manualForm.answer.split(",").map((item) => item.trim());
  }
  return [];
};

// 多选答案转换：数组 -> 字符串
const manualSetMultipleAnswer = (checkedValues) => {
  manualForm.answer = checkedValues.join(",");
};

// 重置手动录入表单
const resetManualForm = () => {
  if (manualFormRef.value) {
    manualFormRef.value.resetFields();
  }
  manualForm.content = "";
  manualForm.difficulty = "medium";
  manualForm.knowledgePoint = "";
  manualForm.answer = "";
  manualForm.analysis = "";
  manualInitOptions();
};

// 打开手动录入抽屉
const manualEntry = (groupId) => {
  const group = questionList.value.find((g) => g.id === groupId);
  if (!group) return;
  manualGroupId.value = groupId;
  resetManualForm();
  manualDrawerVisible.value = true;
};

// 提交手动录入
const handleManualSubmit = async () => {
  if (!manualFormRef.value) return;
  try {
    await manualFormRef.value.validate();
  } catch {
    return;
  }

  const groupType = manualTargetGroup.value?.type;

  // 单选/多选题需校验选项内容非空
  if (groupType === "single" || groupType === "multiple") {
    const hasEmpty = manualForm.options.some((opt) => !opt.content.trim());
    if (hasEmpty) {
      ElMessage.warning("请填写所有选项内容");
      return;
    }
  }

  manualSubmitting.value = true;
  try {
    // 构造与后端 QuestionCompleteDTO 一致的数据
    const submitData = {
      content: manualForm.content,
      type: groupType,
      difficulty: manualForm.difficulty,
      knowledgePoint: manualForm.knowledgePoint,
      options:
        groupType === "single" || groupType === "multiple"
          ? manualForm.options.map((opt) => ({
              label: opt.label,
              content: opt.content,
            }))
          : [],
      answer: manualForm.answer,
      analysis: manualForm.analysis,
    };

    // 调用后端入库，获取真实题目 id
    const res = await addQuestionComplete(submitData);
    const realId = res.data;

    const newQuestion = {
      id: realId,
      content: manualForm.content,
      type: groupType,
      difficulty: manualForm.difficulty,
      knowledgePoint: manualForm.knowledgePoint,
      options: submitData.options,
      answer: manualForm.answer,
      analysis: manualForm.analysis,
    };

    manualTargetGroup.value.questions.push(newQuestion);
    hasUnsavedChanges.value = true;
    ElMessage.success("题目已添加");

    manualDrawerVisible.value = false;
    resetManualForm();
  } catch (error) {
    console.error("手动录入失败:", error);
    ElMessage.error("题目添加失败，请重试");
  } finally {
    manualSubmitting.value = false;
  }
};

// 取消手动录入
const handleManualCancel = () => {
  manualDrawerVisible.value = false;
  resetManualForm();
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
const handleQuestionBankConfirm = async () => {
  showQuestionBank.value = false;
  // 直接从共享状态读取结果
  const result = questionBankResult.value;
  if (!result) return;
  const { groupId, questions } = result;
  const group = questionList.value.find((g) => g.id === groupId);
  if (!group || !questions || questions.length === 0) return;

  let addedCount = 0;
  for (const q of questions) {
    // 跳过已存在的题目
    if (group.questions.some((eq) => eq.id === q.id)) continue;
    try {
      // 调用题目管理接口获取完整题目（含选项、答案、解析）
      const res = await getQuestionCompleteById(q.id);
      const full = res.data;
      group.questions.push({
        id: full.id,
        content: full.content,
        type: full.type,
        options: full.options || [],
        answer: full.answer || "",
        analysis: full.analysis || "",
        knowledgePoint: full.knowledgePoint || "",
        difficulty: full.difficulty || "",
      });
      addedCount++;
    } catch {
      // 接口不可用时用基础数据兜底
      group.questions.push({
        ...q,
        id: q.id || Date.now() + Math.random(),
        options: q.options || [],
        answer: q.answer || "",
      });
      addedCount++;
    }
  }
  if (addedCount > 0) {
    ElMessage.success(`已添加 ${addedCount} 道题目`);
    hasUnsavedChanges.value = true;
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
      hasUnsavedChanges.value = true;
      ElMessage.success("删除成功");
    })
    .catch(() => {});
};

// ===== 完成 =====
const handleComplete = async () => {
  try {
    // 组装题目数据（按 PaperQuestionDTO 格式：questionId, sortOrder, score）
    // sortOrder 全局连续编号，不按分组重置
    let globalSeq = 0;
    const questionsData = questionList.value.flatMap((group) =>
      group.questions.map((q) => ({
        questionId: q.id,
        sortOrder: ++globalSeq,
        score: group.score,
      }))
    );

    if (route.params.id === "new") {
      // 新增试卷：一次性提交试卷信息 + 题目
      await addPaper({
        name: paperData.value.name,
        description: paperData.value.description,
        totalScore: totalScore.value,
        questions: questionsData,
      });
    } else {
      // 编辑已有试卷：调用 updatePaper 提交完整数据
      await updatePaper({
        id: paperData.value.id,
        name: paperData.value.name,
        totalScore: totalScore.value,
        description: paperData.value.description,
        questions: questionsData,
      });
    }

    hasUnsavedChanges.value = false;
    ElMessage.success("试卷保存成功");
  } catch (error) {
    console.warn("保存失败:", error.message);
    ElMessage.error(error.message || "保存失败，请重试");
    return; // 失败时不跳转，留在当前页
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
  window.addEventListener("beforeunload", beforeUnloadHandler);
});

onBeforeUnmount(() => {
  window.removeEventListener("beforeunload", beforeUnloadHandler);
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

    <!-- 试卷信息展示（可编辑） -->
    <el-card class="info-card" shadow="hover" v-if="paperData">
      <div class="paper-info">
        <div class="info-item">
          <span class="info-label">试卷名称：</span>
          <el-input
            v-model="paperData.name"
            placeholder="请输入试卷名称"
            class="info-input"
            @change="hasUnsavedChanges = true"
          />
        </div>
        <div class="info-item info-item-wide">
          <span class="info-label">试卷说明：</span>
          <el-input
            v-model="paperData.description"
            placeholder="请输入试卷说明"
            class="info-input"
            @change="hasUnsavedChanges = true"
          />
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
                v-for="q in group.questions"
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
                    <span class="q-seq">{{ globalQuestionSeq[q.id] }}.</span>
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

    <!-- 手动录入抽屉 -->
    <el-drawer
      v-model="manualDrawerVisible"
      title="手动录入题目"
      direction="rtl"
      size="600px"
      :close-on-click-modal="false"
      @close="handleManualCancel"
    >
      <el-form
        ref="manualFormRef"
        :model="manualForm"
        :rules="manualRules"
        label-width="100px"
        class="manual-form"
      >
        <!-- 题目内容 -->
        <el-form-item label="题目内容" prop="content">
          <el-input
            v-model="manualForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入题目内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 题型（只读，跟随所在分组） -->
        <el-form-item label="题型">
          <el-tag
            :type="getTypeTagType(manualTargetGroup?.type)"
            size="default"
          >
            {{ typeMap[manualTargetGroup?.type] || "未知题型" }}
          </el-tag>
        </el-form-item>

        <!-- 难度 -->
        <el-form-item label="难度">
          <el-select
            v-model="manualForm.difficulty"
            placeholder="请选择难度"
            style="width: 100%"
          >
            <el-option label="简单" value="easy">
              <el-tag type="success" effect="plain">简单</el-tag>
            </el-option>
            <el-option label="中等" value="medium">
              <el-tag type="warning" effect="plain">中等</el-tag>
            </el-option>
            <el-option label="困难" value="hard">
              <el-tag type="danger" effect="plain">困难</el-tag>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 知识点 -->
        <el-form-item label="所属知识点">
          <el-input
            v-model="manualForm.knowledgePoint"
            placeholder="请输入所属知识点"
            clearable
          />
        </el-form-item>

        <!-- 选项配置（单选、多选） -->
        <template
          v-if="
            manualTargetGroup?.type === 'single' ||
            manualTargetGroup?.type === 'multiple'
          "
        >
          <el-form-item label="选项配置">
            <div class="options-container">
              <div
                v-for="(option, index) in manualForm.options"
                :key="index"
                class="option-item"
              >
                <el-tag class="option-label">{{ option.label }}</el-tag>
                <el-input
                  v-model="option.content"
                  placeholder="请输入选项内容"
                  class="option-input"
                />
                <el-button
                  type="danger"
                  link
                  :icon="Delete"
                  @click="manualDeleteOption(index)"
                  :disabled="manualForm.options.length <= 2"
                />
              </div>
              <el-button
                type="primary"
                link
                :icon="Plus"
                @click="manualAddOption"
                class="add-option-btn"
              >
                添加选项
              </el-button>
            </div>
          </el-form-item>

          <!-- 正确答案（单选） -->
          <el-form-item
            v-if="manualTargetGroup?.type === 'single'"
            label="正确答案"
            prop="answer"
          >
            <el-radio-group v-model="manualForm.answer">
              <el-radio
                v-for="option in manualForm.options"
                :key="option.label"
                :label="option.label"
              >
                {{ option.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 正确答案（多选） -->
          <el-form-item
            v-else-if="manualTargetGroup?.type === 'multiple'"
            label="正确答案"
            prop="answer"
          >
            <el-checkbox-group
              :model-value="manualGetMultipleAnswerArray()"
              @update:model-value="manualSetMultipleAnswer"
            >
              <el-checkbox
                v-for="option in manualForm.options"
                :key="option.label"
                :label="option.label"
              >
                {{ option.label }}
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </template>

        <!-- 正确答案（判断） -->
        <el-form-item
          v-else-if="manualTargetGroup?.type === 'judge'"
          label="正确答案"
          prop="answer"
        >
          <el-radio-group v-model="manualForm.answer">
            <el-radio label="true">正确</el-radio>
            <el-radio label="false">错误</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 参考答案（简答） -->
        <el-form-item
          v-else-if="manualTargetGroup?.type === 'essay'"
          label="参考答案"
          prop="answer"
        >
          <el-input
            v-model="manualForm.answer"
            type="textarea"
            :rows="4"
            placeholder="请输入参考答案"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <!-- 题目解析 -->
        <el-form-item label="题目解析">
          <el-input
            v-model="manualForm.analysis"
            type="textarea"
            :rows="3"
            placeholder="请输入题目解析（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="handleManualCancel">取消</el-button>
          <el-button
            type="primary"
            @click="handleManualSubmit"
            :loading="manualSubmitting"
          >
            添加
          </el-button>
        </div>
      </template>
    </el-drawer>
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
  gap: 24px;
  flex-wrap: wrap;
  align-items: center;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item-wide {
  flex: 1 1 100%;
}

.info-label {
  color: #909399;
  font-size: 14px;
  white-space: nowrap;
  flex-shrink: 0;
}

.info-input {
  width: 220px;
}

.info-input-number {
  width: 140px;
}

.info-unit {
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
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

/* 手动录入抽屉 */
.manual-form {
  padding: 20px;
}

.options-container {
  width: 100%;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.option-label {
  min-width: 30px;
  text-align: center;
}

.option-input {
  flex: 1;
}

.add-option-btn {
  margin-top: 10px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-right: 20px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-textarea__inner) {
  resize: vertical;
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
