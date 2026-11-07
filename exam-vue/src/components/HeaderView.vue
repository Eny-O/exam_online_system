<template>
  <header class="app-header">
    <div class="header-content">
      <!-- 右侧：用户信息和退出 -->
      <div class="header-right">
        <div class="user-info">
          <el-icon :size="20" class="icon-user"><User /></el-icon>
          <span class="username">{{ displayUsername }}</span>
        </div>

        <button class="logout-btn" @click="handleLogout">
          <el-icon :size="18"><SwitchButton /></el-icon>
          <span>退出</span>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { User, SwitchButton } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import { resetRouter } from "@/router";
import { ElMessage } from "element-plus";

const router = useRouter();
const userStore = useUserStore();

// 从 store 获取用户名，优先 realName，其次 username
const displayUsername = computed(() => {
  return userStore.userInfo?.realName || userStore.userInfo?.username || "考生";
});

// 处理退出
const handleLogout = async () => {
  try {
    await userStore.logout();
    ElMessage.success("退出成功");
  } catch (error) {
    ElMessage.error(error.message || "退出失败");
  } finally {
    resetRouter();
    router.push("/login");
  }
};
</script>

<style scoped>
.app-header {
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  padding: 1rem 1.5rem;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo-box {
  width: 2.5rem;
  height: 2.5rem;
  background-color: #2563eb;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  color: #ffffff;
  font-weight: bold;
  font-size: 1.125rem;
}

.app-title {
  font-size: 1.25rem;
  font-weight: bold;
  color: #1f2937;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #4b5563;
}

.icon-user {
  color: inherit;
}

.username {
  font-weight: 500;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  color: #dc2626;
  background-color: transparent;
  border: 1px solid transparent;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
  font-size: 1rem;
}

.logout-btn:hover {
  background-color: #fef2f2;
}

.logout-btn span {
  font-size: 1rem;
}
</style>
