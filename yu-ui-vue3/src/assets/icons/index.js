/**
 * Vue3/Vite 迁移：替代 webpack 的 svg-sprite-loader + require.context。
 * 用 import.meta.glob 以 ?raw 方式内联读取 svg/ 目录下所有图标，
 * 运行时拼装成隐藏 <svg><symbol id="icon-xxx"> 雪碧图注入文档，
 * SvgIcon 组件保持 <use xlink:href="#icon-xxx"> 的用法不变。
 */
const rawModules = import.meta.glob('./svg/*.svg', { eager: true, query: '?raw', import: 'default' })

function buildSprite() {
  if (typeof document === 'undefined') return
  const symbols = []
  for (const path in rawModules) {
    const name = path.split('/').pop().replace('.svg', '')
    let content = rawModules[path]
    // 提取 <svg> 内的原始图形与 viewBox
    const viewBoxMatch = content.match(/viewBox="([^"]+)"/)
    const viewBox = viewBoxMatch ? viewBoxMatch[1] : '0 0 1024 1024'
    // 去掉外层 <svg ...> 与 </svg>，保留内部节点
    content = content.replace(/<svg[^>]*>/, '').replace(/<\/svg>/, '')
    symbols.push(`<symbol id="icon-${name}" viewBox="${viewBox}">${content}</symbol>`)
  }
  const container = document.createElement('div')
  container.setAttribute('aria-hidden', 'true')
  container.style.cssText = 'position:absolute;width:0;height:0;overflow:hidden;'
  container.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">${symbols.join('')}</svg>`
  document.body.insertBefore(container, document.body.firstChild)
}

buildSprite()
