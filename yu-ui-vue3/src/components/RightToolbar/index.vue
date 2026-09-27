<template>
  <div class="top-right-btn" :style="style">
    <el-row>
      <el-tooltip class="item" effect="dark" :content="showSearch ? '隐藏搜索' : '显示搜索'" placement="top" v-if="search">
        <el-button size="small" circle :icon="Search" @click="toggleSearch()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="刷新" placement="top">
        <el-button size="small" circle :icon="Refresh" @click="refresh()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="显隐列" placement="top" v-if="Object.keys(columns).length > 0">
        <el-button size="small" circle :icon="Menu" @click="showColumn()" v-if="showColumnsType == 'transfer'" />
        <el-dropdown trigger="click" :hide-on-click="false" style="padding-left: 12px" v-if="showColumnsType == 'checkbox'">
          <el-button size="small" circle :icon="Menu" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>
                <el-checkbox :indeterminate="isIndeterminate" v-model="isChecked" @change="toggleCheckAll"> 列展示 </el-checkbox>
              </el-dropdown-item>
              <div class="check-line"></div>
              <template v-for="(item, key) in columns" :key="key">
                <el-dropdown-item>
                  <el-checkbox v-model="item.visible" @change="checkboxChange($event, key)" :label="item.label" />
                </el-dropdown-item>
              </template>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-tooltip>
    </el-row>
    <el-dialog :title="title" v-model="open" append-to-body>
      <el-transfer
        :titles="['显示', '隐藏']"
        v-model="value"
        :data="transferData"
        @change="dataChange"
      />
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移：el-icon-* 类名 → @element-plus/icons-vue 组件；slot="dropdown" → <template #dropdown>；
// :visible.sync → v-model；size="mini" → small。逻辑与列显隐记忆保持与 Vue2 一致。
import cache from '@/plugins/cache'
import { markRaw } from 'vue'
import { Search, Refresh, Menu } from '@element-plus/icons-vue'

export default {
  name: 'RightToolbar',
  props: {
    showSearch: { type: Boolean, default: true },
    columns: { type: [Array, Object], default: () => ({}) },
    search: { type: Boolean, default: true },
    showColumnsType: { type: String, default: 'checkbox' },
    gutter: { type: Number, default: 10 },
    storageKey: { type: String, default: '' }
  },
  emits: ['update:showSearch', 'queryTable'],
  data() {
    return {
      Search: markRaw(Search),
      Refresh: markRaw(Refresh),
      Menu: markRaw(Menu),
      value: [],
      title: '显示/隐藏',
      open: false
    }
  },
  computed: {
    style() {
      const ret = {}
      if (this.gutter) {
        ret.marginRight = `${this.gutter / 2}px`
      }
      return ret
    },
    isChecked: {
      get() {
        return Array.isArray(this.columns)
          ? this.columns.every((col) => col.visible)
          : Object.values(this.columns).every((col) => col.visible)
      },
      set() {}
    },
    isIndeterminate() {
      return Array.isArray(this.columns)
        ? this.columns.some((col) => col.visible) && !this.isChecked
        : Object.values(this.columns).some((col) => col.visible) && !this.isChecked
    },
    transferData() {
      if (Array.isArray(this.columns)) {
        return this.columns.map((item, index) => ({ key: index, label: item.label }))
      }
      return Object.keys(this.columns).map((key, index) => ({ key: index, label: this.columns[key].label }))
    }
  },
  created() {
    if (this.storageKey) {
      try {
        const saved = cache.local.getJSON(this.storageKey)
        if (saved && typeof saved === 'object') {
          if (Array.isArray(this.columns)) {
            this.columns.forEach((col, index) => {
              if (saved[index] !== undefined) col.visible = saved[index]
            })
          } else {
            Object.keys(this.columns).forEach((key) => {
              if (saved[key] !== undefined) this.columns[key].visible = saved[key]
            })
          }
        }
      } catch (e) {}
    }
    if (this.showColumnsType == 'transfer') {
      if (Array.isArray(this.columns)) {
        for (let item in this.columns) {
          if (this.columns[item].visible === false) {
            this.value.push(parseInt(item))
          }
        }
      } else {
        Object.keys(this.columns).forEach((key, index) => {
          if (this.columns[key].visible === false) {
            this.value.push(index)
          }
        })
      }
    }
  },
  methods: {
    toggleSearch() {
      let el = this.$el
      let formEl = null
      while ((el = el.parentElement) && el !== document.body) {
        if ((formEl = el.querySelector('.el-form'))) break
      }
      if (!formEl) return this.$emit('update:showSearch', !this.showSearch)
      this._animateSearch(formEl, this.showSearch)
    },
    _animateSearch(el, isHide) {
      const DURATION = 260
      const TRANSITION = 'max-height 0.25s ease, opacity 0.2s ease'
      const clear = () => Object.assign(el.style, { transition: '', maxHeight: '', opacity: '', overflow: '' })
      Object.assign(el.style, { overflow: 'hidden', transition: '' })
      if (isHide) {
        Object.assign(el.style, { maxHeight: el.scrollHeight + 'px', opacity: '1', transition: TRANSITION })
        requestAnimationFrame(() => Object.assign(el.style, { maxHeight: '0', opacity: '0' }))
        setTimeout(() => {
          this.$emit('update:showSearch', false)
          clear()
        }, DURATION)
      } else {
        this.$emit('update:showSearch', true)
        this.$nextTick(() => {
          Object.assign(el.style, { maxHeight: '0', opacity: '0' })
          requestAnimationFrame(() =>
            requestAnimationFrame(() => {
              Object.assign(el.style, { transition: TRANSITION, maxHeight: el.scrollHeight + 'px', opacity: '1' })
            })
          )
          setTimeout(clear, DURATION)
        })
      }
    },
    refresh() {
      this.$emit('queryTable')
    },
    dataChange(data) {
      if (Array.isArray(this.columns)) {
        for (let item in this.columns) {
          const key = this.columns[item].key
          this.columns[item].visible = !data.includes(key)
        }
      } else {
        Object.keys(this.columns).forEach((key, index) => {
          this.columns[key].visible = !data.includes(index)
        })
      }
      this.saveStorage()
    },
    showColumn() {
      this.open = true
    },
    checkboxChange(event, key) {
      if (Array.isArray(this.columns)) {
        this.columns.filter((item) => item.key == key)[0].visible = event
      } else {
        this.columns[key].visible = event
      }
      this.saveStorage()
    },
    toggleCheckAll() {
      const newValue = !this.isChecked
      if (Array.isArray(this.columns)) {
        this.columns.forEach((col) => (col.visible = newValue))
      } else {
        Object.values(this.columns).forEach((col) => (col.visible = newValue))
      }
      this.saveStorage()
    },
    saveStorage() {
      if (!this.storageKey) return
      try {
        let state = {}
        if (Array.isArray(this.columns)) {
          this.columns.forEach((col, index) => {
            state[index] = col.visible
          })
        } else {
          Object.keys(this.columns).forEach((key) => {
            state[key] = this.columns[key].visible
          })
        }
        cache.local.setJSON(this.storageKey, state)
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.el-transfer__button) {
  border-radius: 50%;
  padding: 12px;
  display: block;
  margin-left: 0px;
}
:deep(.el-transfer__button:first-child) {
  margin-bottom: 10px;
}
.check-line {
  width: 90%;
  height: 1px;
  background-color: #ccc;
  margin: 3px auto;
}
</style>
