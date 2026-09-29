<template>
  <!-- U3 统一空态：空数据引导占位，支持自定义文案/图标尺寸与引导按钮插槽 -->
  <el-empty :description="description" :image-size="imageSize">
    <template v-if="$slots.default" #default>
      <slot />
    </template>
  </el-empty>
</template>

<script>
export default {
  name: 'AppEmpty',
  props: {
    /** 空态文案（默认按场景给出引导） */
    description: { type: String, default: '暂无数据' },
    /** 插图尺寸 */
    imageSize: { type: Number, default: 120 }
  }
}
</script>
