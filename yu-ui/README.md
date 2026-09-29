# 【已归档 / 停用】管理端 Vue2 工程

> **归档说明（2026-09，U6 管理端 Vue3 迁移收口）**
>
> 本工程的 9 个业务域（system/monitor/tool/brm/aem/sam/oa/dis/tpm，共 119 个业务页面）已**全量迁移**至 [`yu-ui-vue3`](../yu-ui-vue3)（Vite5 + Vue3.5 + Element Plus 2.8），并逐域通过 `vite build` 与运行时截图核验。日常开发与生产部署请使用 `yu-ui-vue3`，**请勿在本工程继续迭代新功能**。
>
> - **源码保护红线**：本工程源码完整留存，不删除、不改写，仅做本冻结标注；作为回退与对照基线长期保留。
> - **如需回退/对照构建**：下述开发与构建命令仍然可用（历史文档原样保留）。
> - 入口路由已切换：`bin/start-dev-test.ps1` 等启动入口现指向 `yu-ui-vue3`（dev 端口 82）。

---

## 开发

```bash
# 克隆项目
git clone https://gitee.com/y_project/RuoYi-Vue

# 进入项目目录
cd ruoyi-ui

# 安装依赖
npm install

# 建议不要直接使用 cnpm 安装依赖，会有各种诡异的 bug。可以通过如下操作解决 npm 下载速度慢的问题
npm install --registry=https://registry.npmmirror.com

# 启动服务
npm run dev
```

浏览器访问 http://localhost:80

## 发布

```bash
# 构建测试环境
npm run build:stage

# 构建生产环境
npm run build:prod
```