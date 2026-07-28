<script setup>
import { ref, reactive, watch } from "vue";
import { Plus, Delete } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { addQuestionComplete, updateQuestionComplete } from "@/api/questionManager";

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  editData: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(["update:visible", "success"]);

const drawerVisible = ref(false);
const formRef = ref(null);
const submitting = ref(false);

// 表单数据
const formData = reactive({
  id: null,
  content: "",
  type: "single",
  difficulty: "medium",
  knowledgePoint: "",
  options: [],
  answer: "",
  analysis: "",
});

// 表单验证规则
const rules = {
  content: [
    { required: true, message: "请输入题目内容", trigger: "blur" },
    { min: 5, message: "题目内容至少5个字符", trigger: "blur" },
  ],
  type: [{ required: true, message: "请选择题型", trigger: "change" }],
  difficulty: [{ required: true, message: "请选择难度", trigger: "change" }],
  knowledgePoint: [
    { required: true, message: "请输入所属知识点", trigger: "blur" },
  ],
  answer: [{ required: true, message: "请输入正确答案", trigger: "blur" }],
};

// 监听抽屉显示状态
watch(
  () => props.visible,
  (val) => {
    drawerVisible.value = val;
    if (val) {
      if (props.editData) {
        // 编辑模式，填充完整数据（含选项）
        Object.assign(formData, {
          id: props.editData.id,
          content: props.editData.content,
          type: props.editData.type,
          difficulty: props.editData.difficulty,
          knowledgePoint: props.editData.knowledgePoint,
          answer: props.editData.answer,
          analysis: props.editData.analysis,
          options: props.editData.options ? [...props.editData.options] : [],
        });
      } else {
        // 新增模式，重置并初始化选项
        resetForm();
      }
    }
  }
);

// 监听抽屉关闭
watch(drawerVisible, (val) => {
  if (!val) {
    emit("update:visible", false);
  }
});

// 初始化选项（单选题默认4个选项）
const initOptions = () => {
  formData.options = [
    { label: "A", content: "" },
    { label: "B", content: "" },
    { label: "C", content: "" },
    { label: "D", content: "" },
  ];
  formData.answer = "";
};

// 题型改变时重置选项和答案
const handleTypeChange = () => {
  initOptions();
};

// 添加选项
const addOption = () => {
  const labels = ["A", "B", "C", "D", "E", "F", "G", "H"];
  const nextLabel = labels[formData.options.length];
  if (nextLabel) {
    formData.options.push({ label: nextLabel, content: "" });
  } else {
    ElMessage.warning("最多只能添加8个选项");
  }
};

// 删除选项
const deleteOption = (index) => {
  if (formData.options.length <= 2) {
    ElMessage.warning("至少保留2个选项");
    return;
  }
  formData.options.splice(index, 1);
  // 重新计算标签
  const labels = ["A", "B", "C", "D", "E", "F", "G", "H"];
  formData.options.forEach((opt, idx) => {
    opt.label = labels[idx];
  });
};

// 获取多选题答案数组
const getMultipleAnswerArray = () => {
  if (formData.type === "multiple" && formData.answer) {
    return formData.answer.split(",").map((item) => item.trim());
  }
  return [];
};

// 设置多选题答案
const setMultipleAnswer = (checkedValues) => {
  formData.answer = checkedValues.join(",");
};

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();

    // 验证选项内容
    if (formData.type !== "essay") {
      const hasEmptyOption = formData.options.some(
        (opt) => !opt.content.trim()
      );
      if (hasEmptyOption) {
        ElMessage.warning("请填写所有选项内容");
        return;
      }
    }

    submitting.value = true;

    // 构造提交数据，新增时不传 id
    const submitData = {
      content: formData.content,
      type: formData.type,
      difficulty: formData.difficulty,
      knowledgePoint: formData.knowledgePoint,
      answer: formData.answer,
      analysis: formData.analysis,
      options: formData.options.map((opt) => ({
        label: opt.label,
        content: opt.content,
      })),
    };

    // 编辑时带上 id
    if (formData.id) {
      submitData.id = formData.id;
    }

    if (formData.id) {
      await updateQuestionComplete(submitData);
      ElMessage.success("修改成功");
    } else {
      await addQuestionComplete(submitData);
      ElMessage.success("新增成功");
    }

    drawerVisible.value = false;
    emit("success");
  } catch (error) {
    console.error("提交失败:", error);
  } finally {
    submitting.value = false;
  }
};

// 取消
const handleCancel = () => {
  drawerVisible.value = false;
};

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields();
  }
  formData.id = null;
  formData.content = "";
  formData.type = "single";
  formData.difficulty = "medium";
  formData.knowledgePoint = "";
  formData.answer = "";
  formData.analysis = "";
  initOptions();
};
</script>

<template>
  <el-drawer
    v-model="drawerVisible"
    :title="editData ? '编辑题目' : '新增题目'"
    direction="rtl"
    size="600px"
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      class="question-form"
    >
      <!-- 题目内容 -->
      <el-form-item label="题目内容" prop="content">
        <el-input
          v-model="formData.content"
          type="textarea"
          :rows="4"
          placeholder="请输入题目内容"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <!-- 题型 -->
      <el-form-item label="题型" prop="type">
        <el-select
          v-model="formData.type"
          placeholder="请选择题型"
          style="width: 100%"
          @change="handleTypeChange"
        >
          <el-option label="单选题" value="single" />
          <el-option label="多选题" value="multiple" />
          <el-option label="判断题" value="judge" />
          <el-option label="简答题" value="essay" />
        </el-select>
      </el-form-item>

      <!-- 难度 -->
      <el-form-item label="难度" prop="difficulty">
        <el-select
          v-model="formData.difficulty"
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
      <el-form-item label="所属知识点" prop="knowledgePoint">
        <el-input
          v-model="formData.knowledgePoint"
          placeholder="请输入所属知识点"
          clearable
        />
      </el-form-item>

      <!-- 选项配置（单选、多选） -->
      <template
        v-if="formData.type === 'single' || formData.type === 'multiple'"
      >
        <el-form-item label="选项配置">
          <div class="options-container">
            <div
              v-for="(option, index) in formData.options"
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
                @click="deleteOption(index)"
                :disabled="formData.options.length <= 2"
              />
            </div>
            <el-button
              type="primary"
              link
              :icon="Plus"
              @click="addOption"
              class="add-option-btn"
            >
              添加选项
            </el-button>
          </div>
        </el-form-item>

        <!-- 正确答案（单选） -->
        <el-form-item
          v-if="formData.type === 'single'"
          label="正确答案"
          prop="answer"
        >
          <el-radio-group v-model="formData.answer">
            <el-radio
              v-for="option in formData.options"
              :key="option.label"
              :label="option.label"
            >
              {{ option.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 正确答案（多选） -->
        <el-form-item
          v-else-if="formData.type === 'multiple'"
          label="正确答案"
          prop="answer"
        >
          <el-checkbox-group
            :model-value="getMultipleAnswerArray()"
            @update:model-value="setMultipleAnswer"
          >
            <el-checkbox
              v-for="option in formData.options"
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
        v-else-if="formData.type === 'judge'"
        label="正确答案"
        prop="answer"
      >
        <el-radio-group v-model="formData.answer">
          <el-radio label="true">正确</el-radio>
          <el-radio label="false">错误</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 参考答案（简答） -->
      <el-form-item
        v-else-if="formData.type === 'essay'"
        label="参考答案"
        prop="answer"
      >
        <el-input
          v-model="formData.answer"
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
          v-model="formData.analysis"
          type="textarea"
          :rows="3"
          placeholder="请输入题目解析（选填）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <!-- 底部按钮 -->
    <template #footer>
      <div class="drawer-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ editData ? "保存" : "确定" }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.question-form {
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
