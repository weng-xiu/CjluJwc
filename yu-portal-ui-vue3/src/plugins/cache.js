/**
 * E1 门户请求层能力对齐：会话级缓存（防重复提交依赖），与管理端 plugins/cache.js 同源。
 * 门户此前无 plugins 目录，仅落地请求层用到的 session 部分。
 */
const sessionCache = {
  set(key, value) {
    if (!sessionStorage) return
    if (key != null && value != null) {
      sessionStorage.setItem(key, value)
    }
  },
  get(key) {
    if (!sessionStorage) return null
    if (key == null) return null
    return sessionStorage.getItem(key)
  },
  setJSON(key, jsonValue) {
    if (jsonValue != null) {
      this.set(key, JSON.stringify(jsonValue))
    }
  },
  getJSON(key) {
    const value = this.get(key)
    if (value != null) {
      return JSON.parse(value)
    }
    return null
  },
  remove(key) {
    sessionStorage.removeItem(key)
  }
}

export default {
  session: sessionCache
}
