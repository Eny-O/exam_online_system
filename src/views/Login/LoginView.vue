<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { User, Lock, View, Hide } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import { ElMessage } from "element-plus";

const router = useRouter();
const userStore = useUserStore();

const loginFormRef = ref(null);
const showPassword = ref(false);
const loading = ref(false);

const formData = reactive({
  username: "",
  password: "",
});

// Element Plus 表单校验规则
const rules = reactive({
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 2, max: 20, message: "用户名长度为 2-20 个字符", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于 6 位", trigger: "blur" },
  ],
});

const handleSubmit = async () => {
  // 先校验表单
  const valid = await loginFormRef.value.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    await userStore.login(formData);
    ElMessage.success("登录成功");
    router.push("/");
  } catch (error) {
    ElMessage.error(error.message || "登录失败，请检查用户名和密码");
  } finally {
    loading.value = false;
  }
};

const handleRegister = () => {
  router.push("/register");
};
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">
          <span>E</span>
        </div>
        <h1 class="title">欢迎回来</h1>
        <p class="subtitle">登录您的考试系统账号</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="formData"
        :rules="rules"
        @submit.prevent="handleSubmit"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username" class="form-item">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password" class="form-item">
          <el-input
            v-model="formData.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
          >
            <template #suffix>
              <el-icon
                class="cursor-pointer"
                @click="showPassword = !showPassword"
              >
                <View v-if="showPassword" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="login-btn"
          :loading="loading"
          @click="handleSubmit"
        >
          登录
        </el-button>
      </el-form>

      <div class="register-link">
        <span class="text-gray">还没有账号？</span>
        <el-link type="primary" @click="handleRegister" class="ml-1">
          立即注册
        </el-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #eff6ff 0%, #e0e7ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.login-card {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1),
    0 10px 10px -5px rgba(0, 0, 0, 0.04);
  padding: 2rem;
  width: 100%;
  max-width: 28rem;
}

.login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.logo-icon {
  width: 4rem;
  height: 4rem;
  background-color: #2563eb;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
}

.logo-icon span {
  color: white;
  font-size: 1.5rem;
  font-weight: bold;
}

.title {
  font-size: 1.5rem;
  font-weight: bold;
  color: #111827;
  margin: 0;
}

.subtitle {
  color: #6b7280;
  margin-top: 0.5rem;
}

.login-form {
  margin-bottom: 1.5rem;
}

.form-item {
  margin-bottom: 1.25rem;
}

.form-item :deep(.el-form-item__label) {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.5rem;
}

.cursor-pointer {
  cursor: pointer;
}

.login-btn {
  width: 100%;
  margin-top: 0.5rem;
}

.register-link {
  text-align: center;
  margin-top: 1.5rem;
}

.text-gray {
  color: #4b5563;
}

.ml-1 {
  margin-left: 0.25rem;
}
</style>
