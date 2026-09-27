<template>
  <div :style="'height:' + height" v-loading="loading" element-loading-text="正在加载页面，请稍候！">
    <iframe :id="iframeId" style="width: 100%; height: 100%" :src="src" frameborder="no"></iframe>
  </div>
</template>

<script>
// Vue3 迁移：移除 IE attachEvent 分支，仅保留标准 onload。
export default {
  name: 'InnerLink',
  props: {
    src: { type: String, default: '/' },
    iframeId: { type: String }
  },
  data() {
    return {
      loading: false,
      height: document.documentElement.clientHeight - 94.5 + 'px;'
    }
  },
  mounted() {
    const iframeId = ('#' + this.iframeId).replace(/\//g, '\\/')
    const iframe = document.querySelector(iframeId)
    if (iframe) {
      this.loading = true
      iframe.onload = () => {
        this.loading = false
      }
    }
  }
}
</script>
