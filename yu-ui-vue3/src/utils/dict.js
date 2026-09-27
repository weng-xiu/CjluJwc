/**
 * Vue3 迁移：替代若依原 DictData(DataDict) 插件 + this.dict.type.xxx 用法。
 * 通过全局 mixin 读取组件的 `dicts: ['sys_xxx']` 选项，异步拉取字典并挂载到
 * `this.dict.type`。业务页模板 `v-for="d in dict.type.sys_yes_no"` 零改写。
 * 字典项字段：label/value 及 element 风格 raw.listClass/cssClass（与 DictTag 约定）。
 */
import { ref, shallowRef } from 'vue'
import { getDicts } from '@/api/system/dict/data'
import store from '@/store'

// 全局字典缓存，key 为字典类型
const dictCache = {}

function normalize(res) {
  const data = res.data || res.rows || []
  return data.map((d) => ({
    label: d.dictLabel,
    value: d.dictValue,
    elTagType: d.listClass,
    elTagClass: d.cssClass,
    raw: d
  }))
}

function searchStore(type) {
  const list = store.getters.dict || []
  for (let i = 0; i < list.length; i++) {
    if (list[i].key === type) return list[i].value
  }
  return null
}

function loadDict(type) {
  if (dictCache[type]) return Promise.resolve(dictCache[type])
  const storeDict = searchStore(type)
  if (storeDict) {
    dictCache[type] = normalize({ data: storeDict })
    return Promise.resolve(dictCache[type])
  }
  return getDicts(type).then((res) => {
    store.dispatch('dict/setDict', { key: type, value: res.data })
    dictCache[type] = normalize(res)
    return dictCache[type]
  })
}

// 组合式用法（可选，供 <script setup> 页面）
export function useDict(...types) {
  const res = ref({})
  types.forEach((type) => {
    res.value[type] = shallowRef([])
    loadDict(type).then((list) => {
      res.value[type] = list
    })
  })
  return res.value
}

export default {
  install(app) {
    app.mixin({
      data() {
        return {
          dict: { type: {} }
        }
      },
      created() {
        let types = this.$options.dicts
        // 支持函数式 dicts（如 MasterDetailPanel 依据 columns 动态收集字典类型）
        if (typeof types === 'function') {
          types = types.call(this)
        }
        if (Array.isArray(types) && types.length) {
          types.forEach((type) => {
            this.dict.type[type] = []
            loadDict(type).then((list) => {
              this.dict.type[type] = list
            })
          })
        }
      }
    })
  }
}
