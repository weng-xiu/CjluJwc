<template>
  <div class="dict-tag">
    <template v-for="(item, index) in displayArray">
      <el-tag
        v-if="item !== undefined && item !== null && item !== ''"
        :key="index"
        :type="tagType(item)"
        :class="tagClass(item)"
        class="dict-tag__item"
      >{{ tagLabel(item) }}</el-tag>
    </template>
  </div>
</template>

<script>
// Vue3 迁移：Vue2 的 filters:{handleArray} 已移除，改由 computed + methods 处理。
// 兼容 options 元素字段：{label, value, raw:{listClass,cssClass}}（与 utils/dict 归一化结构一致）
export default {
  name: 'DictTag',
  props: {
    options: { type: Array, default: () => [] },
    // 单值或数组
    value: { type: [String, Number, Array, Boolean], default: undefined },
    // 分隔符（当 value 为逗号分隔字符串时）
    separator: { type: String, default: ',' },
    // 未匹配到字典项时是否直接展示原始值
    showValue: { type: Boolean, default: true }
  },
  computed: {
    // 将 value 归一化为数组
    valueArray() {
      if (Array.isArray(this.value)) {
        return this.value
      }
      if (this.value === undefined || this.value === null || this.value === '') {
        return []
      }
      return String(this.value).split(this.separator)
    },
    displayArray() {
      return this.valueArray.map((val) => {
        const matched = this.options.find(
          (item) => String(item.value) === String(val) || String(item.dictValue) === String(val)
        )
        if (matched) {
          return matched
        }
        // 未匹配：showValue 时展示原始值，否则过滤掉
        return this.showValue ? { label: val, _raw: true } : null
      }).filter((item) => item !== null && !item._raw || item)
    }
  },
  methods: {
    isMatched(item) {
      return !item._raw
    },
    tagLabel(item) {
      return item.label !== undefined ? item.label : item.dictLabel
    },
    tagType(item) {
      const cls = item.raw ? item.raw.listClass : item.listClass
      if (!cls || cls === 'default') {
        return 'info'
      }
      return cls
    },
    tagClass(item) {
      const css = item.raw ? item.raw.cssClass : item.cssClass
      return css || ''
    }
  }
}
</script>

<style scoped>
.dict-tag {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
}
.dict-tag__item + .dict-tag__item {
  margin-left: 4px;
}
</style>
