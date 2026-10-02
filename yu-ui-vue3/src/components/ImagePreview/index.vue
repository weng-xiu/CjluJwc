<template>
  <el-image
    :src="`${realSrc}`"
    fit="cover"
    :style="`width:${realWidth};height:${realHeight};`"
    :preview-src-list="realSrcList"
    preview-teleported
  >
    <template #error>
      <div class="image-slot">
        <el-icon><Picture /></el-icon>
      </div>
    </template>
  </el-image>
</template>

<script>
// Vue3 迁移：<div slot="error"> → <template #error>；字体图标 <i class="el-icon-picture-outline"> →
// <el-icon><Picture/></el-icon>；process.env.VUE_APP_BASE_API → import.meta.env.VITE_APP_BASE_API；
// 追加 preview-teleported，避免图片预览浮层被表格单元格的 overflow 裁剪。
// 颜色改用 --dt-* 令牌，暗色模式自动换肤（U1）。
import { isExternal } from "@/utils/validate"

export default {
  name: "ImagePreview",
  props: {
    src: {
      type: String,
      default: ""
    },
    width: {
      type: [Number, String],
      default: ""
    },
    height: {
      type: [Number, String],
      default: ""
    }
  },
  computed: {
    realSrc() {
      if (!this.src) {
        return
      }
      const realSrc = this.src.split(",")[0]
      if (isExternal(realSrc)) {
        return realSrc
      }
      return import.meta.env.VITE_APP_BASE_API + realSrc
    },
    realSrcList() {
      if (!this.src) {
        return
      }
      return this.src.split(",").map(item => {
        return isExternal(item) ? item : import.meta.env.VITE_APP_BASE_API + item
      })
    },
    realWidth() {
      return typeof this.width === "string" ? this.width : `${this.width}px`
    },
    realHeight() {
      return typeof this.height === "string" ? this.height : `${this.height}px`
    }
  }
}
</script>

<style lang="scss" scoped>
.el-image {
  border-radius: var(--dt-radius-base);
  background-color: var(--dt-fill-light);
  box-shadow: var(--dt-shadow-light);
  :deep(.el-image__inner) {
    transition: all 0.3s;
    cursor: pointer;
    &:hover {
      transform: scale(1.2);
    }
  }
  :deep(.image-slot) {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    color: var(--dt-text-secondary);
    font-size: 30px;
  }
}
</style>
