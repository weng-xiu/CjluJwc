# syntax=docker/dockerfile:1
# CjluJwc 管理端前端镜像：Node 构建 → Nginx 托管
# 构建上下文为 yu-ui-vue3/（见 docker-compose.yml build.context）

# ---------- 构建阶段 ----------
FROM node:18-alpine AS builder
WORKDIR /build

# 先拷贝依赖清单利用层缓存；npm ci 严格按 lock 安装（与 CI / 门户端同源），lock 漂移当场失败
COPY package.json package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com

COPY . .
RUN npm run build:prod

# ---------- 运行阶段 ----------
FROM nginx:1.25-alpine
ENV TZ=Asia/Shanghai

COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO /dev/null http://127.0.0.1/ || exit 1
