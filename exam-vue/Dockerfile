# 多阶段构建：node 构建静态资源 -> nginx 托管
# 构建上下文为仓库根（CI 中 context: .，file: exam-vue/Dockerfile），
# 因此 COPY 源路径以 exam-vue/ 开头。
# 必须用 Node >= 20.11：unplugin@2.3.11 的 index.mjs 在模块加载时执行
# resolve(import.meta.dirname, ...)，而 import.meta.dirname 是 Node 20.11+ 才有的属性；
# 低于此版本（如 node:18）会是 undefined，导致 build 直接报 paths[0] must be of type string。
FROM node:22 AS build
WORKDIR /app
COPY exam-vue/package*.json ./
RUN npm ci
COPY exam-vue/ .
RUN npm run build

FROM nginx:1.27-alpine
# 前端为哈希路由（createWebHashHistory），无需 SPA rewrite；
# nginx 仅做静态托管 + 把 /api 代理到网关容器（gateway:8080）。
COPY exam-vue/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
