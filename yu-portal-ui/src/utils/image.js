/**
 * 图片地址处理工具
 */

/**
 * 判断是否为外部链接
 * @param {string} path
 * @returns {boolean}
 */
export function isExternal(path) {
  return /^(https?:|mailto:|tel:|data:)/.test(path)
}

/**
 * 拼接图片完整访问地址
 * 后端返回的图片路径为相对路径（如 /profile/upload/xxx.png），
 * 需要拼接 VUE_APP_BASE_API 前缀走代理访问后端
 * @param {string} path 图片路径
 * @returns {string} 完整图片地址
 */
export function imgUrl(path) {
  if (!path) {
    return ''
  }
  if (isExternal(path)) {
    return path
  }
  return process.env.VUE_APP_BASE_API + path
}
