<template>
  <el-select
    :value="value"
    filterable
    remote
    :remote-method="handleRemoteSearch"
    :loading="loading"
    :clearable="clearable"
    :disabled="disabled"
    :placeholder="placeholder"
    :style="style"
    :multiple="multiple"
    v-bind="$attrs"
    @input="handleInput"
    @visible-change="handleVisibleChange"
  >
    <el-option
      v-for="opt in displayOptions"
      :key="optKey(opt)"
      :label="optLabel(opt)"
      :value="opt[valueKey]"
    />
  </el-select>
</template>

<script>
/**
 * A7/N7「下拉禁全量拉取」通用远程下拉。
 *
 * 背景：管理端多个大实体（教师/学生/开课/课程库/教室/班级）下拉原先一次性 pageSize:1000
 * 全量拉取，数据量大时拖慢首屏且占用内存。本组件按 accountSync 已验证的远程搜索范式封装：
 * 仅在首次展开时预载首页（默认 20 条），用户输入时以指定 searchKey 走服务端 name LIKE 过滤。
 *
 * 设计要点：
 * - 自持状态（options/loading 内聚），避免同页多个 el-select 复用同一 options 数组互相污染；
 * - 编辑回显：外部传入 initialOption {value,label}，当选中值不在当前远程结果集中时合成一条置顶，
 *   保证回填表单显示名称而非裸 ID。
 */
export default {
  name: 'RemoteSelect',
  inheritAttrs: false,
  props: {
    // v-model 绑定的选中主键
    value: {
      type: [Number, String, Array],
      default: null
    },
    // 列表加载函数，签名 (query) => Promise<{ rows | data }>
    api: {
      type: Function,
      required: true
    },
    // 远程关键字写入的查询字段名（如 teacherName / courseName / studentName / classroomName）
    searchKey: {
      type: String,
      default: ''
    },
    // 选项值字段（如 teacherId / courseId / offeringId / classroomId / studentId）
    valueKey: {
      type: String,
      default: 'id'
    },
    // 选项展示字段（与 formatter 二选一，formatter 优先）
    labelKey: {
      type: String,
      default: ''
    },
    // 自定义展示格式化函数 (row) => string
    formatter: {
      type: Function,
      default: null
    },
    // 固定附加查询参数（如按院系/学期预筛）
    params: {
      type: Object,
      default() {
        return {}
      }
    },
    pageSize: {
      type: Number,
      default: 20
    },
    // 编辑回显：{ value, label }，选中项不在远程结果集时用于合成展示项
    initialOption: {
      type: Object,
      default: null
    },
    placeholder: {
      type: String,
      default: '请输入关键字搜索'
    },
    clearable: {
      type: Boolean,
      default: true
    },
    disabled: {
      type: Boolean,
      default: false
    },
    multiple: {
      type: Boolean,
      default: false
    },
    style: {
      type: [String, Object],
      default: ''
    }
  },
  data() {
    return {
      options: [],
      loading: false,
      loadedOnce: false,
      _searchTimer: null
    }
  },
  computed: {
    displayOptions() {
      const list = this.options.slice()
      const selected = this.value
      const hasValue = Array.isArray(selected)
        ? selected.length > 0
        : (selected !== null && selected !== undefined && selected !== '')
      if (hasValue && !Array.isArray(selected) && this.initialOption && this.initialOption.value != null
          && String(this.initialOption.value) === String(selected)) {
        const exists = list.some(o => String(o[this.valueKey]) === String(selected))
        if (!exists) {
          list.unshift({ __synthetic: true, __label: this.initialOption.label, [this.valueKey]: this.initialOption.value })
        }
      }
      return list
    }
  },
  beforeDestroy() {
    if (this._searchTimer) clearTimeout(this._searchTimer)
  },
  methods: {
    optKey(opt) {
      return opt[this.valueKey]
    },
    optLabel(opt) {
      if (opt.__synthetic) return opt.__label
      if (this.formatter) return this.formatter(opt)
      if (this.labelKey) return opt[this.labelKey]
      return opt[this.valueKey]
    },
    handleInput(val) {
      this.$emit('input', val)
      // change 附带选中行对象与展示名（合成回显项也能取到 label），供页面需同步冗余名称列的场景消费（如 meetingRoom 管理员 adminName）；未监听的第二/三参向后兼容
      const opt = Array.isArray(val) ? null : this.displayOptions.find(o => String(o[this.valueKey]) === String(val)) || null
      this.$emit('change', val, opt, opt ? this.optLabel(opt) : '')
    },
    handleVisibleChange(visible) {
      // 首次展开时预载首页，避免挂载即请求（多表单页可减少并发压力）
      if (visible && !this.loadedOnce && !this.loading) {
        this.fetch('')
      }
    },
    handleRemoteSearch(query) {
      // 输入防抖，避免逐字符打到后端
      if (this._searchTimer) clearTimeout(this._searchTimer)
      this._searchTimer = setTimeout(() => {
        this.fetch(query)
      }, 300)
    },
    fetch(keyword) {
      this.loading = true
      const query = { pageNum: 1, pageSize: this.pageSize, ...this.params }
      const kw = (keyword || '').trim()
      if (this.searchKey && kw) {
        query[this.searchKey] = kw
      }
      return this.api(query).then(res => {
        this.options = (res && (res.rows || res.data)) || []
        this.loadedOnce = true
      }).catch(() => {
        this.options = []
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
