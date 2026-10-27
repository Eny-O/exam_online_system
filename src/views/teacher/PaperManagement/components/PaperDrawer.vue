<script setup>
import { ref, reactive, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { updatePaper } from "@/api/paperManager";

const router = useRouter();

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
  name: "",
  description: "",
});

// 表单验证规则
const rules = {
  name: [
    { required: true, message: "请输入试卷名称", trigger: "blur" },
    { min: 2, max: 50, message: "长度在 2 到 50 个字符", trigger: "blur" },
  ],
};

// 监听抽屉显示状态
watch(
  () => props.visible,
  (val) => {
    drawerVisible.value = val;
    if (val) {
      if (props.editData) {
        // 编辑模式
        Object.assign(formData, {
          id: props.editData.id,
          name: props.editData.name,
          description: props.editData.description || "",
        });
      } else {
        // 新增模式
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

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();

    submitting.value = true;

    const submitData = {
      name: formData.name,
      description: formData.description,
    };

    if (formData.id) {
      submitData.id = formData.id;
      await updatePaper(submitData);
      ElMessage.success("修改成功");
      drawerVisible.value = false;
      emit("success");
    } else {
      // 新增：不调后端接口，携带表单数据跳转到组卷页面，由"完成"按钮统一提交
      drawerVisible.value = false;
      emit("success");
      router.push({
        path: "/teacher/paper-content/new",
        query: {
          name: submitData.name,
          description: submitData.description,
        },
      });
    }
  } catch (error) {
    console.error("提交失败:", error);
    ElMessage.error(error.message || "操作失败");
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
  formData.name = "";
  formData.description = "";
};
</script>

<template>
  <el-drawer
    v-model="drawerVisible"
    :title="editData ? '编辑试卷' : '新增试卷'"
    direction="rtl"
    size="520px"
    :close-on-click-modal="false"
  >
    <div class="paper-drawer-container">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
        class="paper-form"
      >
        <!-- 试卷名称 -->
        <el-form-item label="试卷名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="请输入试卷名称"
            maxlength="50"
            show-word-limit
            clearable
          />
        </el-form-item>

        <!-- 描述 -->
        <el-form-item label="试卷描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="5"
            placeholder="请输入试卷描述（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <div class="drawer-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ editData ? "保存" : "开始组卷" }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.paper-drawer-container {
  height: 100%;
  overflow-y: auto;
}

.paper-form {
  padding: 10px;
}

.unit-text {
  margin-left: 10px;
  color: #909399;
  font-size: 14px;
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
