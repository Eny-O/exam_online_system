<template>
  <aside class="sidebar-container">
    <!-- 头部 Logo 区域 -->
    <div class="sidebar-header">
      <h1 class="app-title">
        <div class="logo-box">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <span>在线考试系统</span>
      </h1>
    </div>

    <!-- 导航菜单区域 -->
    <nav class="sidebar-nav">
      <el-menu
        :default-active="activePath"
        class="custom-menu"
        background-color="#0f172a"
        text-color="#94a3b8"
        active-text-color="#ffffff"
        router
      >
        <template v-for="item in menuItems" :key="item.path || item.title">
          <!-- 情况 A: 有子菜单 -> 渲染 el-sub-menu -->
          <el-sub-menu
            v-if="item.children && item.children.length"
            :index="item.title"
            class="menu-group"
          >
            <template #title>
              <el-icon :size="20">
                <component :is="item.icon" />
              </el-icon>
              <span>{{ item.title }}</span>
            </template>

            <!-- 子菜单项 -->
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
              class="menu-item-custom sub-item"
            >
              <el-icon :size="20">
                <component :is="child.icon" />
              </el-icon>
              <span>{{ child.label }}</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 情况 B: 无子菜单 -> 直接渲染 el-menu-item -->
          <el-menu-item
            v-else
            :index="item.path"
            class="menu-item-custom top-level-item"
          >
            <el-icon :size="20">
              <component :is="item.icon" />
            </el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </nav>

    <!-- 底部退出按钮 -->
    <div class="sidebar-footer">
      <button class="logout-btn" @click="handleLogout">
        <el-icon :size="20"><SwitchButton /></el-icon>
        <span>退出登录</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { resetRouter } from "@/router";
import { Document, SwitchButton } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

// 从 store 中获取菜单数据，映射为模板需要的格式
const menuItems = computed(() => {
  return userStore.menuList.map((item) => ({
    ...item,
    label: item.title,
    children: item.children
      ? item.children.map((child) => ({
          ...child,
          label: child.title,
        }))
      : null,
  }));
});

// 计算当前激活的路由路径
const activePath = computed(() => route.path);

// 退出登录处理
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
/* 容器基础样式 */
.sidebar-container {
  width: 16rem;
  height: 100vh;
  background-color: #0f172a;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  animation: slideIn 0.3s ease-out forwards;
}

@keyframes slideIn {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(0);
  }
}

/* 头部样式 */
.sidebar-header {
  padding: 1.5rem;
  border-bottom: 1px solid #334155;
}

.app-title {
  font-size: 1.25rem;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 0;
}

.logo-box {
  width: 2.5rem;
  height: 2.5rem;
  background-color: #3b82f6;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

/* 导航区域 */
.sidebar-nav {
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

.sidebar-nav::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

/* --- Element Plus 核心样式覆盖 --- */
.custom-menu {
  border-right: none !important;
  background-color: transparent !important;
}

/* 通用菜单项样式 */
.menu-item-custom {
  margin-bottom: 0.5rem;
  border-radius: 0.5rem;
  transition: all 0.2s;
  height: auto;
  line-height: normal;
  padding: 0.75rem 1rem !important;
}

/* 选中状态 */
.custom-menu .el-menu-item.is-active {
  background-color: #3b82f6 !important;
  color: #ffffff !important;
}

/* 悬停状态 */
.custom-menu .el-menu-item:hover {
  background-color: #1e293b !important;
  color: #ffffff !important;
}

/* 一级菜单缩进 */
.top-level-item {
  padding-left: 1rem !important;
}

.menu-group {
  padding-left: 0 !important;
  margin-bottom: 0.5rem;
}

.sub-item {
  padding-left: 3rem !important;
  margin-top: 0.25rem;
  margin-bottom: 0.25rem;
}

/* 底部区域 */
.sidebar-footer {
  padding: 1rem;
  border-top: 1px solid #334155;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.75rem 1rem;
  background: transparent;
  border: none;
  color: #94a3b8;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.2s;
  font-size: inherit;
}

.logout-btn:hover {
  background-color: #1e293b;
  color: #ffffff;
}
</style>
