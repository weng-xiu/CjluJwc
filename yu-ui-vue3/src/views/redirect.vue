<template>
  <div></div>
</template>

<script>
// 刷新页签中转：/redirect/:path 命中后跳转到真实路径，触发组件重建。
export default {
  name: 'Redirect',
  created() {
    const { params, query } = this.$route
    const { path } = params
    this.$router.replace({ path: '/' + path, query })
  }
}
</script>
