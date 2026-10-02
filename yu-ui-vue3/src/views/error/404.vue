<template>
  <div class="not-found">
    <div class="code">404</div>
    <div class="desc">抱歉，你访问的页面不存在</div>
    <el-button type="primary" @click="$router.push('/index')">返回首页</el-button>
  </div>
</template>

<script>
export default { name: 'PageNotFound' }
</script>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: var(--dt-bg-page);
}
.code {
  font-size: 120px;
  font-weight: 700;
  color: var(--dt-color-primary);
  line-height: 1;
}
.desc {
  margin: 24px 0;
  font-size: 20px;
  color: var(--dt-text-regular);
}
</style>
