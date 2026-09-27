<template>
  <div class="not-found">
    <div class="code">401</div>
    <div class="desc">抱歉，你无权访问该页面</div>
    <el-button type="primary" @click="$router.push('/index')">返回首页</el-button>
  </div>
</template>

<script>
export default { name: 'Unauthorized' }
</script>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f0f2f5;
}
.code {
  font-size: 120px;
  font-weight: 700;
  color: #e6a23c;
  line-height: 1;
}
.desc {
  margin: 24px 0;
  font-size: 20px;
  color: #606266;
}
</style>
