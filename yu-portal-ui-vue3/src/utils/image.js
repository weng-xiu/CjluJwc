/**
 * 图片地址处理工具
 */

/** 判断是否为外部链接 */
export function isExternal(path) {
  return /^(https?:|mailto:|tel:|data:)/.test(path)
}

/**
 * 拼接图片完整访问地址
 * 后端返回相对路径（如 /profile/upload/xxx.png），拼 VITE_APP_BASE_API 前缀走代理
 * Vue3/Vite 迁移：process.env.VUE_APP_BASE_API → import.meta.env.VITE_APP_BASE_API
 */
export function imgUrl(path) {
  if (!path) {
    return ''
  }
  if (isExternal(path)) {
    return path
  }
  return import.meta.env.VITE_APP_BASE_API + path
}
