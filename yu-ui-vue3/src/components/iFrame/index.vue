<template>
  <!-- Vue2→Vue3 迁移：模板无破坏性写法，保持原实现 -->
  <div v-loading="loading" :style="'height:' + height">
    <iframe
      :src="src"
      frameborder="no"
      style="width: 100%; height: 100%"
      scrolling="auto"
    />
  </div>
</template>
<script>
export default {
  props: {
    src: {
      type: String,
      required: true
    },
  },
  data() {
    return {
      height: document.documentElement.clientHeight - 94.5 + "px;",
      loading: true,
      url: this.src
    }
  },
  mounted: function () {
    setTimeout(() => {
      this.loading = false
    }, 300)
    const that = this
    window.onresize = function temp() {
      that.height = document.documentElement.clientHeight - 94.5 + "px;"
    }
  }
}
</script>
