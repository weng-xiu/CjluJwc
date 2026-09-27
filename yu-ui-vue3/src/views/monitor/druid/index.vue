<template>
  <!-- Vue2→Vue3 迁移：process.env.VUE_APP_BASE_API → import.meta.env.VITE_APP_BASE_API；iFrame 组件补 .vue 后缀 -->
  <i-frame :src="url" />
</template>
<script>
import iFrame from "@/components/iFrame/index.vue"
export default {
  name: "Druid",
  components: { iFrame },
  data() {
    return {
      url: import.meta.env.VITE_APP_BASE_API + "/druid/login.html"
    }
  }
}
</script>
