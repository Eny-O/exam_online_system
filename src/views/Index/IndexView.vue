<script setup>
import HeaderView from "@/components/HeaderView.vue";
import SidebarView from "@/components/SidebarView.vue";
import { RouterView } from "vue-router";
</script>
<template>
  <div class="common-layout">
    <el-container>
      <el-aside>
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
