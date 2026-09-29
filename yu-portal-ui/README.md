# 【已归档 / 停用】门户端 Vue2 工程

> **归档说明（2026-09，U6 双栈收口）**
>
> 门户端（登录/框架/首页 + 20 个 PC 业务页 + 5 个公开门户页 + 12 个移动端页，共 37 页）已**全量迁移**至 [`yu-portal-ui-vue3`](../yu-portal-ui-vue3)（Vite5 + Vue3.5 + Element Plus 2.8），`vite build` 通过并逐页运行时核验。日常开发与部署请使用 `yu-portal-ui-vue3`（dev 端口 81），**请勿在本工程继续迭代新功能**。
>
> - **源码保护红线**：本工程源码完整留存，不删除、不改写，仅做本冻结标注（本 README 为归档新增文件，非原有源码）；作为回退与对照基线长期保留。
> - **如需回退/对照构建**：历史命令仍可用——`npm install` 后 `npm run dev`（webpack4，Node ≥17 需 `$env:NODE_OPTIONS="--openssl-legacy-provider"`），生产构建 `npm run build:prod`。
