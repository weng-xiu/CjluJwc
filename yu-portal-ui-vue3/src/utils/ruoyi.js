/**
 * E1 门户请求层能力对齐：与管理端 utils/ruoyi.js 同源的通用工具（仅移植请求层所需）。
 * 保持函数实现与管理端逐字一致，确保 GET 参数序列化口径、blob 校验行为双端统一。
 */

// 参数处理：将嵌套对象展开为 a[b]=v 形式，跳过 null/空串/undefined（与管理端 tansParams 完全一致）
export function tansParams(params) {
  let result = ''
  for (const propName of Object.keys(params)) {
    const value = params[propName]
    var part = encodeURIComponent(propName) + "="
    if (value !== null && value !== "" && typeof (value) !== "undefined") {
      if (typeof value === 'object') {
        for (const key of Object.keys(value)) {
          if (value[key] !== null && value[key] !== "" && typeof (value[key]) !== 'undefined') {
            let params = propName + '[' + key + ']'
            var subPart = encodeURIComponent(params) + "="
            result += subPart + encodeURIComponent(value[key]) + "&"
          }
        }
      } else {
        result += part + encodeURIComponent(value) + "&"
      }
    }
  }
  return result
}

// 验证是否为 blob（导出接口返回 application/json 说明是错误对象而非文件流）
export function blobValidate(data) {
  return data.type !== 'application/json'
}
