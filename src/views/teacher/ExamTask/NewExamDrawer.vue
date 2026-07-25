<template>
  <div class="exam-create-form-container">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      class="exam-form"
    >
      <el-tabs v-model="activeTab" type="border-card">
        <!-- Tab 1: 基本信息设置 -->
        <el-tab-pane label="基本信息设置" name="basic">
          <el-form-item label="考试名称" prop="examName">
            <el-input
              v-model="formData.examName"
              placeholder="请输入考试名称"
              maxlength="100"
              show-word-limit
              clearable
            />
          </el-form-item>

          <el-form-item label="关联试卷" prop="paperId">
            <el-select
              v-model="formData.paperId"
              placeholder="请选择关联试卷"
              filterable
              style="width: 100%"
              clearable
            >
              <el-option
                v-for="paper in paperList"
                :key="paper.id"
                :label="paper.name"
                :value="paper.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="参考班级" prop="classIds">
            <el-select
              v-model="formData.classIds"
              placeholder="请选择参考班级"
              multiple
              collapse-tags
              collapse-tags-tooltip
              style="width: 100%"
              clearable
            >
              <el-option
                v-for="cls in classList"
                :key="cls.id"
                :label="cls.name"
                :value="cls.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="考试时间" prop="examTimeRange">
            <el-date-picker
              v-model="formData.examTimeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="考试时长" prop="duration">
            <el-input-number
              v-model="formData.duration"
              :min="10"
              :max="300"
              :step="5"
              placeholder="请输入时长"
              style="width: 200px"
            />
            <span class="unit-text">分钟</span>
          </el-form-item>
        </el-tab-pane>

        <!-- Tab 2: 组卷策略 -->
        <el-tab-pane label="组卷策略" name="strategy">
          <el-form-item label="组卷方式" prop="strategyType">
            <el-radio-group v-model="formData.strategyType" size="large">
              <el-radio-button label="manual">手动组卷</el-radio-button>
              <el-radio-button label="random">随机组卷</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <!-- 手动组卷 -->
          <div
            v-if="formData.strategyType === 'manual'"
            class="manual-strategy"
          >
            <el-alert
              title="手动组卷说明"
              description="请从题库中挑选具体题目组成试卷，您可以精确控制每道题目的内容和顺序。"
              type="info"
              :closable="false"
              style="margin-bottom: 20px"
            />

            <el-table
              :data="manualQuestions"
              border
              stripe
              style="width: 100%; margin-bottom: 20px"
            >
              <el-table-column
                prop="questionType"
                label="题型"
                width="120"
                align="center"
              >
                <template #default="{ row }">
                  <el-tag :type="getQuestionTypeTag(row.questionType)">
                    {{ getQuestionTypeText(row.questionType) }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column
                prop="count"
                label="题目数量"
                width="120"
                align="center"
              />

              <el-table-column
                prop="totalScore"
                label="总分"
                width="120"
                align="center"
              >
                <template #default="{ row }">
                  <span class="score-text">{{ row.totalScore }}分</span>
                </template>
              </el-table-column>

              <el-table-column label="操作" align="center">
                <template #default="{ $index }">
                  <el-button
                    link
                    type="primary"
                    size="small"
                    @click="handleEditManualQuestion($index)"
                  >
                    <el-icon><Edit /></el-icon>
                    编辑
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    size="small"
                    @click="handleDeleteManualQuestion($index)"
                  >
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-empty
              v-if="manualQuestions.length === 0"
              description="暂无题目，请点击下方按钮从题库挑选"
              :image-size="100"
            />

            <div class="manual-actions">
              <el-button type="primary" @click="handleSelectFromBank">
                <el-icon><Plus /></el-icon>
                从题库挑选题目
              </el-button>
            </div>
          </div>

          <!-- 随机组卷 -->
          <div v-else class="random-strategy">
            <el-alert
              title="随机组卷说明"
              description="系统将根据您设置的规则，从题库中随机抽取题目组成试卷。每次考试题目可能不同。"
              type="success"
              :closable="false"
              style="margin-bottom: 20px"
            />

            <div class="random-rules-list">
              <el-card
                v-for="(rule, index) in formData.randomRules"
                :key="index"
                class="random-rule-card"
                shadow="hover"
              >
                <template #header>
                  <div class="card-header">
                    <span class="rule-title">抽题规则 {{ index + 1 }}</span>
                    <el-button
                      link
                      type="danger"
                      size="small"
                      @click="handleDeleteRandomRule(index)"
                    >
                      <el-icon><Delete /></el-icon>
                      删除规则
                    </el-button>
                  </div>
                </template>

                <el-form
                  :ref="(el) => setRuleFormRef(el, index)"
                  :model="rule"
                  :rules="randomRuleRules"
                  label-width="100px"
                  class="rule-form"
                >
                  <el-row :gutter="20">
                    <el-col :span="6">
                      <el-form-item
                        label="题型"
                        :prop="[index, 'questionType']"
                      >
                        <el-select
                          v-model="rule.questionType"
                          placeholder="选择题型"
                          style="width: 100%"
                        >
                          <el-option label="单选题" value="single" />
                          <el-option label="多选题" value="multiple" />
                          <el-option label="判断题" value="judge" />
                        </el-select>
                      </el-form-item>
                    </el-col>

                    <el-col :span="6">
                      <el-form-item label="题目数量" :prop="[index, 'count']">
                        <el-input-number
                          v-model="rule.count"
                          :min="1"
                          :max="100"
                          placeholder="数量"
                          style="width: 100%"
                        />
                      </el-form-item>
                    </el-col>

                    <el-col :span="6">
                      <el-form-item
                        label="单题分值"
                        :prop="[index, 'scorePerQuestion']"
                      >
                        <el-input-number
                          v-model="rule.scorePerQuestion"
                          :min="1"
                          :max="100"
                          :precision="1"
                          placeholder="分值"
                          style="width: 100%"
                        />
                      </el-form-item>
                    </el-col>

                    <el-col :span="6">
                      <el-form-item
                        label="难度系数"
                        :prop="[index, 'difficulty']"
                      >
                        <el-select
                          v-model="rule.difficulty"
                          placeholder="选择难度"
                          style="width: 100%"
                        >
                          <el-option label="简单" value="easy" />
                          <el-option label="中等" value="medium" />
                          <el-option label="困难" value="hard" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </el-form>
              </el-card>

              <el-empty
                v-if="formData.randomRules.length === 0"
                description="暂无抽题规则，请点击下方按钮添加"
                :image-size="100"
              />
            </div>

            <div class="random-actions">
              <el-button
                type="primary"
                plain
                @click="handleAddRandomRule"
                class="add-rule-btn"
              >
                <el-icon><Plus /></el-icon>
                添加抽题规则
              </el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-form>
    <QuestionSelector
      v-model="selectorVisible"
      @confirm="
        (questions) => {
          // 这里处理选中的题目，比如添加到 manualQuestions 列表
          console.log('选中的题目：', questions);
          ElMessage.success(`成功添加 ${questions.length} 道题目`);
        }
      "
    />
    <QuestionEditor
      v-model="editorVisible"
      :edit-data="currentEditData"
      @confirm="handleConfirmEdit"
    />
  </div>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import { Plus, Delete } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import QuestionSelector from "@/views/teacher/QuestionManagement/components/QuestionSelector.vue";
import QuestionEditor from "@/views/teacher/QuestionManagement/components/QuestionEditor.vue";

// 定义事件
const emit = defineEmits(["success"]);

// 定义props
const props = defineProps({
  editData: {
    type: Object,
    default: null,
  },
});

//手动选题弹窗ref
const selectorVisible = ref(false);

// 题目编辑弹窗
const editorVisible = ref(false);
const currentEditIndex = ref(-1);
const currentEditData = ref({});

// Tabs激活状态
const activeTab = ref("basic");

// 表单引用
const formRef = ref(null);

// 动态规则表单引用数组
const ruleFormRefs = ref([]);

// 表单数据
const formData = reactive({
  examName: "",
  paperId: null,
  classIds: [],
  examTimeRange: [],
  duration: 120,
  strategyType: "manual", // manual 或 random
  randomRules: [],
});

// 手动组卷题目列表（Mock数据）
const manualQuestions = ref([
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
]);

// 表单验证规则
const formRules = {
  examName: [
    { required: true, message: "请输入考试名称", trigger: "blur" },
    { min: 2, max: 100, message: "长度在 2 到 100 个字符", trigger: "blur" },
  ],
  paperId: [{ required: true, message: "请选择关联试卷", trigger: "change" }],
  classIds: [
    {
      required: true,
      message: "请至少选择一个参考班级",
      trigger: "change",
      type: "array",
    },
  ],
  examTimeRange: [
    {
      required: true,
      message: "请选择考试时间范围",
      trigger: "change",
      type: "array",
    },
  ],
  duration: [
    { required: true, message: "请输入考试时长", trigger: "blur" },
    {
      type: "number",
      min: 10,
      max: 300,
      message: "时长范围为10-300分钟",
      trigger: "blur",
    },
  ],
  strategyType: [
    { required: true, message: "请选择组卷方式", trigger: "change" },
  ],
};

// 随机组卷规则验证规则
const randomRuleRules = {
  questionType: [{ required: true, message: "选择题型", trigger: "change" }],
  count: [
    { required: true, message: "输入数量", trigger: "blur" },
    {
      type: "number",
      min: 1,
      max: 100,
      message: "数量范围为1-100",
      trigger: "blur",
    },
  ],
  scorePerQuestion: [
    { required: true, message: "输入分值", trigger: "blur" },
    {
      type: "number",
      min: 1,
      max: 100,
      message: "分值范围为1-100",
      trigger: "blur",
    },
  ],
  difficulty: [{ required: true, message: "选择难度", trigger: "change" }],
};

// Mock试卷列表
const paperList = ref([
  { id: 1, name: "Vue3基础知识试卷" },
  { id: 2, name: "JS高级概念测试卷" },
  { id: 3, name: "树与图论专项测试" },
  { id: 4, name: "SQL查询优化试卷" },
  { id: 5, name: "进程管理与内存分配" },
]);

// Mock班级列表
const classList = ref([
  { id: 1, name: "计算机2101班" },
  { id: 2, name: "计算机2102班" },
  { id: 3, name: "软件工程2102班" },
  { id: 4, name: "计算机科学2103班" },
  { id: 5, name: "信息管理2101班" },
  { id: 6, name: "网络工程2101班" },
  { id: 7, name: "大数据2101班" },
  { id: 8, name: "数字媒体2101班" },
]);

// 获取题型标签类型
const getQuestionTypeTag = (type) => {
  const tagMap = {
    single: "primary",
    multiple: "success",
    judge: "warning",
  };
  return tagMap[type] || "info";
};

// 获取题型文本
const getQuestionTypeText = (type) => {
  const textMap = {
    single: "单选题",
    multiple: "多选题",
    judge: "判断题",
  };
  return textMap[type] || "未知";
};

// 设置规则表单引用
const setRuleFormRef = (el, index) => {
  if (el) {
    ruleFormRefs.value[index] = el;
  }
};

// 添加随机组卷规则
const handleAddRandomRule = () => {
  formData.randomRules.push({
    questionType: "",
    count: 1,
    scorePerQuestion: 5,
    difficulty: "medium",
  });
};

// 删除随机组卷规则
const handleDeleteRandomRule = (index) => {
  formData.randomRules.splice(index, 1);
};

// 从题库挑选题目（手动组卷）
const handleSelectFromBank = () => {
  ElMessage.info("打开题库选择对话框（待实现）");
  // TODO: 打开题库选择弹窗
  selectorVisible.value = true;
};

// 编辑手动组卷题目
const handleEditManualQuestion = (index) => {
  ElMessage.info(`编辑第${index + 1}条题目配置（待实现）`);
  currentEditIndex.value = index;
  currentEditData.value = { ...manualQuestions.value[index] };
  editorVisible.value = true;
};

// 确认编辑题目
const handleConfirmEdit = (editedData) => {
  if (currentEditIndex.value >= 0) {
    manualQuestions.value[currentEditIndex.value] = editedData;
    ElMessage.success("题目配置更新成功");
  }
};

// 删除手动组卷题目
const handleDeleteManualQuestion = (index) => {
  manualQuestions.value.splice(index, 1);
  ElMessage.success("删除成功");
};

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields();
  }

  // 重置默认值
  formData.duration = 120;
  formData.strategyType = "manual";
  formData.randomRules = [];
  manualQuestions.value = [];
  ruleFormRefs.value = [];
  activeTab.value = "basic";
};

// 填充表单数据（编辑模式）
const fillFormData = (data) => {
  formData.examName = data.examName || "";
  formData.paperId = data.paperId || null;
  formData.classIds = data.classIds || [];
  formData.duration = data.duration || 120;
  formData.strategyType = data.strategyType || "manual";

  // 处理考试时间范围
  if (data.startTime && data.endTime) {
    formData.examTimeRange = [data.startTime, data.endTime];
  } else {
    formData.examTimeRange = [];
  }

  // 处理组卷策略数据
  if (data.strategyType === "manual") {
    manualQuestions.value = data.manualQuestions || [];
    formData.randomRules = [];
  } else if (data.strategyType === "random") {
    formData.randomRules = data.randomRules || [];
    manualQuestions.value = [];
  }
};
// 提交表单
const submit = async () => {
  if (!formRef.value) return;

  try {
    // 验证基本信息
    await formRef.value.validate();

    // 验证考试时间范围
    if (formData.examTimeRange && formData.examTimeRange.length === 2) {
      const [startTime, endTime] = formData.examTimeRange;
      const start = new Date(startTime);
      const end = new Date(endTime);
      if (end <= start) {
        ElMessage.error("结束时间必须大于开始时间");
        activeTab.value = "basic";
        return;
      }
    }

    // 验证组卷策略
    if (formData.strategyType === "manual") {
      if (manualQuestions.value.length === 0) {
        ElMessage.error("手动组卷至少需要添加一道题目");
        activeTab.value = "strategy";
        return;
      }
    } else if (formData.strategyType === "random") {
      if (formData.randomRules.length === 0) {
        ElMessage.error("随机组卷至少需要添加一条抽题规则");
        activeTab.value = "strategy";
        return;
      }

      // 验证所有随机规则
      for (let i = 0; i < ruleFormRefs.value.length; i++) {
        if (ruleFormRefs.value[i]) {
          try {
            await ruleFormRefs.value[i].validate();
          } catch (error) {
            ElMessage.error(`第${i + 1}条抽题规则填写不完整`);
            activeTab.value = "strategy";
            return;
          }
        }
      }
    }

    // 构建提交数据
    const submitData = {
      ...formData,
      startTime: formData.examTimeRange?.[0] || "",
      endTime: formData.examTimeRange?.[1] || "",
      manualQuestions:
        formData.strategyType === "manual" ? manualQuestions.value : [],
      randomRules:
        formData.strategyType === "random" ? formData.randomRules : [],
    };

    console.log("提交表单数据：", submitData);

    // 模拟API调用
    setTimeout(() => {
      ElMessage.success("考试创建成功");
      emit("success");
      resetForm();
    }, 500);
  } catch (error) {
    ElMessage.error("请检查表单填写是否正确");
    console.error("表单验证失败：", error);
  }
};

// 监听编辑数据变化
watch(
  () => props.editData,
  (newVal) => {
    if (newVal) {
      // 填充表单数据
      fillFormData(newVal);
    } else {
      // 重置表单
      resetForm();
    }
  },
  { immediate: true }
);

// 暴露方法给父组件
defineExpose({
  submit,
  resetForm,
});
</script>

<style scoped>
.exam-create-form-container {
  height: 100%;
  overflow-y: auto;
}

.exam-form {
  padding: 10px;
}

.unit-text {
  margin-left: 10px;
  color: #909399;
  font-size: 14px;
}

/* 手动组卷样式 */
.manual-strategy {
  margin-top: 20px;
}

.manual-actions {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.score-text {
  color: #409eff;
  font-weight: 600;
}

/* 随机组卷样式 */
.random-strategy {
  margin-top: 20px;
}

.random-rules-list {
  min-height: 200px;
}

.random-rule-card {
  margin-bottom: 15px;
  transition: all 0.3s;
}

.random-rule-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rule-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.rule-form {
  padding: 10px 0;
}

.random-actions {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.add-rule-btn {
  border-style: dashed;
  width: 100%;
  padding: 20px;
  font-size: 16px;
}

.add-rule-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

/* Tabs样式优化 */
:deep(.el-tabs--border-card) {
  border: none;
  box-shadow: none;
}

:deep(.el-tabs__content) {
  padding: 20px;
  min-height: 400px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

/* 响应式调整 */
@media (max-width: 768px) {
  :deep(.el-row) {
    flex-direction: column;
  }

  :deep(.el-col) {
    width: 100% !important;
    margin-bottom: 10px;
  }
}
</style>
