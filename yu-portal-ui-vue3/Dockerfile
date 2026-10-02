# syntax=docker/dockerfile:1
# CjluJwc 门户端前端镜像：Node 构建 → Nginx 托管（补齐 V4.0 §7.3 门户自动化部署）
# 构建上下文为 yu-portal-ui-vue3/（见 docker-compose.yml services.portal.build.context）

# ---------- 构建阶段 ----------
FROM node:18-alpine AS builder
WORKDIR /build

# 管理端地址为构建期常量（Vite 读 .env.production）：部署时按实际域名覆盖
#   docker build --build-arg VITE_APP_ADMIN_URL=https://jwc.yangtze.edu.cn .
# 留空时门户点击"管理入口"不会跳转到未知地址，而是停在门户登录页并带 reason 标记
ARG VITE_APP_ADMIN_URL=""
ENV VITE_APP_ADMIN_URL=${VITE_APP_ADMIN_URL}

# 先拷贝依赖清单利用层缓存；npm ci 严格按 lock 安装，与 CI 同源
COPY package.json package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com

# 代码检查在 CI 门禁执行（镜像构建只做编译），避免依赖装好后卡在 lint
COPY . .
RUN npm run build:prod

# ---------- 运行阶段 ----------
FROM nginx:1.25-alpine
ENV TZ=Asia/Shanghai

# 与管理端一致沿用官方 nginx（master 以 root 绑 80，worker 以 nginx 用户运行），
# 容器加固由 compose 的 no-new-privileges 兜底；改用 nginx-unprivileged 需同步调整监听端口，列为后续项
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO /dev/null http://127.0.0.1/ || exit 1
