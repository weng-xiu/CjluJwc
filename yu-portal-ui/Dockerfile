# syntax=docker/dockerfile:1
# CjluJwc Vue2 门户端（README 日常开发入口）镜像：Node 构建 → Nginx 托管（D22 容器化收口）
# 与 yu-portal-ui-vue3 同款两段式；vue-cli 4.4.6/webpack4 与 Node 17+ OpenSSL3 不兼容，固定 node:16
# 构建上下文为 yu-portal-ui/（见 docker-compose.yml services.portal-vue2.build.context）

# ---------- 构建阶段 ----------
FROM node:16-alpine AS builder
WORKDIR /build

# 先拷贝依赖清单利用层缓存；npm ci 严格按 lock 安装，与 Vue3 门户同源
COPY package.json package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com

# 生产 API 前缀走 .env.production（VUE_APP_BASE_API=/prod-api），由下方 nginx 反代到后端
COPY . .
RUN npm run build:prod

# ---------- 运行阶段 ----------
FROM nginx:1.25-alpine
ENV TZ=Asia/Shanghai

# 与管理端/ Vue3 门户一致沿用官方 nginx（master root 绑 80，worker nginx 用户），
# 容器加固由 compose 的 no-new-privileges 兜底
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO /dev/null http://127.0.0.1/ || exit 1
