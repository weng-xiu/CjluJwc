/**
 * E5 表单校验统一 · 公共 rules 工厂（Vue2 主栈 yu-ui）。
 *
 * <p>目标：替代散落在各页面的 "xxx不能为空"/正则硬编码，形成一致提示 + 统一 trigger 语义。
 * 采用工厂函数返回 el-form rule 对象数组，页面按字段拼装 rules 时只需一句 `...required('用户名称')`，
 * 无需重复 message 文案与 trigger 数组，从源头收敛 149 处手写校验的措辞与口径漂移。</p>
 *
 * <p>约定：
 * <ul>
 *   <li>文本类默认 trigger=["blur","change"]，选择类默认 trigger="change"（下拉/单选/多选切换即校验）</li>
 *   <li>required 工厂对字符串走非空判定，数字/布尔由 Element 内建 required 处理</li>
 *   <li>长度/正则类工厂不隐含 required，可与 required 组合成数组使用</li>
 *   <li>密码强度按工程既有约定「≥8 位 + 大小写 + 数字」（同 UserForm.pwdValidator）</li>
 * </ul>
 *
 * @author cjlu
 */

/** 通用文本必填："${label}不能为空"，blur 与 change 双触发。 */
export function required(label, options = {}) {
  return {
    required: true,
    message: options.message || `${label}不能为空`,
    trigger: options.trigger || ['blur', 'change']
  }
}

/** 选择类必填（下拉/单选/多选/日期）：仅 change 触发，避免打开对话框时误报。 */
export function requiredSelect(label, options = {}) {
  return {
    required: true,
    message: options.message || `请选择${label}`,
    trigger: options.trigger || 'change'
  }
}

/** 长度区间：与 required 组合使用；文案沿用工程既有 "xxx长度必须介于 A 和 B 之间"。 */
export function lengthRange(label, min, max) {
  return {
    min,
    max,
    message: `${label}长度必须介于 ${min} 和 ${max} 之间`,
    trigger: ['blur', 'change']
  }
}

/** 邮箱（可空，非空时校验格式）。 */
export function email() {
  return {
    type: 'email',
    message: '请输入正确的邮箱地址',
    trigger: ['blur', 'change']
  }
}

/** 中国大陆手机号（可空，非空时校验 11 位 1 开头）。 */
export function phone() {
  return {
    pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
    message: '请输入正确的手机号码',
    trigger: 'blur'
  }
}

/** 身份证（15/18 位，末位允许 X；可空，非空时校验格式）。 */
export function idCard() {
  return {
    pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/,
    message: '请输入正确的身份证号码',
    trigger: 'blur'
  }
}

/** 密码强度：8~20 位，必含大小写字母与数字（对齐 UserForm.pwdValidator）。 */
export function password(label = '密码') {
  return [
    { min: 8, max: 20, message: `${label}长度必须介于 8 和 20 之间`, trigger: 'blur' },
    {
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/,
      message: `${label}必须包含大小写字母和数字`,
      trigger: 'blur'
    }
  ]
}

/** 正整数（大于 0 的自然数），常用于容量/学分/学时等。 */
export function positiveInt(label) {
  return {
    validator: (rule, value, callback) => {
      if (value === null || value === undefined || value === '') {
        callback()
        return
      }
      const n = Number(value)
      if (!Number.isInteger(n) || n <= 0) {
        callback(new Error(`${label}必须为正整数`))
        return
      }
      callback()
    },
    trigger: ['blur', 'change']
  }
}

/** 非负数（含小数），常用于分数/金额。 */
export function nonNegative(label) {
  return {
    validator: (rule, value, callback) => {
      if (value === null || value === undefined || value === '') {
        callback()
        return
      }
      const n = Number(value)
      if (Number.isNaN(n) || n < 0) {
        callback(new Error(`${label}不能为负数`))
        return
      }
      callback()
    },
    trigger: ['blur', 'change']
  }
}

/** 0~100 百分比：常用作权重占比。 */
export function percent(label = '占比') {
  return {
    validator: (rule, value, callback) => {
      if (value === null || value === undefined || value === '') {
        callback()
        return
      }
      const n = Number(value)
      if (Number.isNaN(n) || n < 0 || n > 100) {
        callback(new Error(`${label}必须在 0~100 之间`))
        return
      }
      callback()
    },
    trigger: ['blur', 'change']
  }
}

export default {
  required,
  requiredSelect,
  lengthRange,
  email,
  phone,
  idCard,
  password,
  positiveInt,
  nonNegative,
  percent
}
