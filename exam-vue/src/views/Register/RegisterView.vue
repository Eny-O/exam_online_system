<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import {
  User,
  Message,
  Lock,
  View,
  Hide,
  ArrowRight,
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";

const router = useRouter();
const showPassword = ref(false);
const showConfirmPassword = ref(false);
const formData = ref({
  username: "",
  email: "",
  password: "",
  confirmPassword: "",
});

const handleSubmit = () => {
  // 验证密码是否一致
  if (formData.value.password !== formData.value.confirmPassword) {
    ElMessage.error("两次输入的密码不一致");
    return;
  }

  // 这里可以添加注册逻辑
  console.log("注册信息:", formData.value);
  ElMessage.success("注册成功");

  // 注册成功后跳转到登录页
  setTimeout(() => {
    router.push("/login");
  }, 1500);
};

const handleLogin = () => {
  router.push("/login");
};
</script>

<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <div class="logo-icon">
          <el-icon :size="32" color="white">
            <User />
          </el-icon>
        </div>
        <h1 class="title">创建账号</h1>
        <p class="subtitle">注册开始您的学习之旅</p>
      </div>

      <el-form @submit.prevent="handleSubmit" class="register-form">
        <el-form-item label="用户名" class="form-item">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="邮箱" class="form-item">
          <el-input
            v-model="formData.email"
            type="email"
            placeholder="请输入邮箱"
            :prefix-icon="Message"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" class="form-item">
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

        <el-form-item label="确认密码" class="form-item">
          <el-input
            v-model="formData.confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            placeholder="请再次输入密码"
            :prefix-icon="Lock"
            size="large"
          >
            <template #suffix>
              <el-icon
                class="cursor-pointer"
                @click="showConfirmPassword = !showConfirmPassword"
              >
                <View v-if="showConfirmPassword" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="register-btn"
          @click="handleSubmit"
        >
          <span>注册</span>
          <el-icon class="ml-1">
            <ArrowRight />
          </el-icon>
        </el-button>
      </el-form>

      <div class="login-link">
        <span class="text-gray">已有账号？</span>
        <el-link type="primary" @click="handleLogin" class="ml-1">
          立即登录
        </el-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #eff6ff 0%, #e0e7ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.register-card {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1),
    0 10px 10px -5px rgba(0, 0, 0, 0.04);
  padding: 2rem;
  width: 100%;
  max-width: 28rem;
}

.register-header {
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

.register-form {
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

.register-btn {
  width: 100%;
  margin-top: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.register-btn:hover {
  transform: scale(1.02);
}

.register-btn:active {
  transform: scale(0.98);
}

.login-link {
  text-align: center;
  margin-top: 1.5rem;
}

.text-gray {
  color: #6b7280;
}

.ml-1 {
  margin-left: 0.25rem;
}
</style>
