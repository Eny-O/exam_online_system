<script setup>
import HeaderView from "@/components/HeaderView.vue";
import SidebarView from "@/components/SidebarView.vue";
import { RouterView } from "vue-router";
</script>
<template>
  <div class="common-layout">
    <el-container>
      <el-aside width="16rem">
        <SidebarView />
      </el-aside>
      <el-container>
        <el-header>
          <HeaderView />
        </el-header>
        <el-main>
          <RouterView />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.common-layout {
  height: 100vh;
  overflow: hidden;
}

.common-layout :deep(.el-container) {
  height: 100%;
}

.common-layout :deep(.el-main) {
  padding: 12px;
  overflow-y: auto;
  height: 0;
  flex: 1;
}

.common-layout :deep(.el-header) {
  padding: 8px 16px;
  flex-shrink: 0;
}
</style>

<style>
/* 全局重置，消除浏览器默认边距，确保全屏布局 */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  overflow: hidden;
}
</style>
