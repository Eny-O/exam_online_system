NEW_FILE_CODE
<template>
  <el-dialog
    v-model="visible"
    title="编辑题目配置"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      class="edit-form"
    >
      <el-form-item label="题型" prop="questionType">
        <el-select
          v-model="formData.questionType"
          placeholder="选择题型"
          style="width: 100%"
          disabled
        >
          <el-option label="单选题" value="single" />
          <el-option label="多选题" value="multiple" />
          <el-option label="判断题" value="judge" />
        </el-select>
        <div class="form-tip">题型不可修改，如需更改请删除后重新添加</div>
      </el-form-item>

      <el-form-item label="题目数量" prop="count">
        <el-input-number
          v-model="formData.count"
          :min="1"
          :max="100"
          placeholder="请输入题目数量"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="单题分值" prop="scorePerQuestion">
        <el-input-number
          v-model="formData.scorePerQuestion"
          :min="1"
          :max="100"
          :precision="1"
          placeholder="请输入单题分值"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="总分">
        <el-input :value="totalScore" disabled style="width: 100%">
          <template #append>分</template>
        </el-input>
        <div class="form-tip">总分 = 题目数量 × 单题分值</div>
      </el-form-item>

      <el-form-item label="难度系数" prop="difficulty">
        <el-select
          v-model="formData.difficulty"
          placeholder="选择难度系数"
          style="width: 100%"
        >
          <el-option label="简单" value="easy">
            <el-tag type="success" size="small">简单</el-tag>
          </el-option>
          <el-option label="中等" value="medium">
            <el-tag type="warning" size="small">中等</el-tag>
          </el-option>
          <el-option label="困难" value="hard">
            <el-tag type="danger" size="small">困难</el-tag>
          </el-option>
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  editData: {
    type: Object,
    default: () => ({}),
  },
});

const emit = defineEmits(["update:modelValue", "confirm"]);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit("update:modelValue", val),
});

const formRef = ref(null);

// 表单数据
const formData = reactive({
  questionType: "",
  count: 1,
  scorePerQuestion: 5,
  difficulty: "medium",
});

// 计算总分
const totalScore = computed(() => {
  return (formData.count * formData.scorePerQuestion).toFixed(1);
});

// 表单验证规则
const formRules = {
  questionType: [{ required: true, message: "选择题型", trigger: "change" }],
  count: [
    { required: true, message: "输入题目数量", trigger: "blur" },
    {
      type: "number",
      min: 1,
      max: 100,
      message: "数量范围为1-100",
      trigger: "blur",
    },
  ],
  scorePerQuestion: [
    { required: true, message: "输入单题分值", trigger: "blur" },
    {
      type: "number",
      min: 1,
      max: 100,
      message: "分值范围为1-100",
      trigger: "blur",
    },
  ],
  difficulty: [{ required: true, message: "选择难度系数", trigger: "change" }],
};

// 获取题型文本
// const getQuestionTypeText = (type) => {
//   const textMap = {
//     single: "单选题",
//     multiple: "多选题",
//     judge: "判断题",
//   };
//   return textMap[type] || "未知";
// };

// 填充表单数据
const fillFormData = (data) => {
  if (data) {
    formData.questionType = data.questionType || "";
    formData.count = data.count || 1;
    formData.scorePerQuestion =
      data.scorePerQuestion || data.totalScore / data.count || 5;
    formData.difficulty = data.difficulty || "medium";
  }
};

// 确认编辑
const handleConfirm = async () => {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();

    // 构建返回数据
    const result = {
      ...formData,
      totalScore: parseFloat(totalScore.value),
    };

    emit("confirm", result);
    handleClose();
  } catch (error) {
    ElMessage.error("请检查表单填写是否正确");
  }
};

// 关闭对话框
const handleClose = () => {
  visible.value = false;
  if (formRef.value) {
    formRef.value.resetFields();
  }
};

// 监听弹窗打开和数据变化
watch([visible, () => props.editData], ([newVisible, newEditData]) => {
  if (newVisible && newEditData) {
    fillFormData(newEditData);
  }
});
</script>

<style scoped>
.edit-form {
  padding: 20px 0;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-input-group__append) {
  background-color: #f5f7fa;
  color: #909399;
}
</style>
