<template>
  <div :class="{ hidden: hidden }" class="pagination-container">
    <!-- Vue3 迁移：el-pagination 的 :current-page.sync/:page-size.sync → 单向 props + @current-change/@size-change 显式 emit -->
    <el-pagination
      :background="background"
      :current-page="page"
      :page-size="limit"
      :layout="layout"
      :page-sizes="pageSizes"
      :pager-count="pagerCount"
      :total="total"
      v-bind="$attrs"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script>
export default {
  name: 'Pagination',
  inheritAttrs: false,
  props: {
    total: { required: true, type: Number },
    page: { type: Number, default: 1 },
    limit: { type: Number, default: 20 },
    pageSizes: { type: Array, default() { return [10, 20, 30, 50] } },
    pagerCount: { type: Number, default: document.body.clientWidth < 992 ? 5 : 7 },
    layout: { type: String, default: 'total, sizes, prev, pager, next, jumper' },
    background: { type: Boolean, default: true },
    autoScroll: { type: Boolean, default: true },
    hidden: { type: Boolean, default: false }
  },
  emits: ['update:page', 'update:limit', 'pagination'],
  methods: {
    handleSizeChange(val) {
      let page = this.page
      if (this.page * val > this.total) {
        page = 1
        this.$emit('update:page', 1)
      }
      this.$emit('update:limit', val)
      this.$emit('pagination', { page, limit: val })
      if (this.autoScroll) this.doScroll()
    },
    handleCurrentChange(val) {
      this.$emit('update:page', val)
      this.$emit('pagination', { page: val, limit: this.limit })
      if (this.autoScroll) this.doScroll()
    },
    doScroll() {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }
}
</script>

<style scoped>
.pagination-container { background: #fff; padding: 8px 0; }
.pagination-container.hidden { display: none; }
</style>
