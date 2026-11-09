# 多阶段构建：node 构建静态资源 -> nginx 托管
# 构建上下文为 exam-vue 目录（compose 中 context: ./exam-vue）
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
# 前端为哈希路由（createWebHashHistory），无需 SPA rewrite；
# nginx 仅做静态托管 + 把 /api 代理到网关容器（gateway:8080）。
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
