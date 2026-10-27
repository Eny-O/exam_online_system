import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    proxy: {
      // 所有 /api 开头的请求代理到 API 网关（:8080）
      // 注意：这里不再剥掉 /api 前缀 —— 由网关的 StripPrefix=1 统一剥离，
      // 保证「开发走 vite 代理」与「生产走 Nginx 代理」的请求路径完全一致。
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
