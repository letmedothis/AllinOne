/**
 * Luckysheet 经典脚本按序加载器(全站共享)。
 *
 * 产物是依赖全局 jQuery 的经典脚本(plugin.js 内含 jQuery,须先于主库执行),
 * 不能作为 ES Module 动态 import:模块作用域下顶层 $ 未定义会中断求值,
 * window.luckysheet 永不挂载。主库须经 /luckysheet/ 固定站点路径加载
 * (vite 插件提供),其内部按相对路径加载 expendPlugins/*。
 */
const LUCKYSHEET_PLUGIN_JS = '/luckysheet/plugins/js/plugin.js'
const LUCKYSHEET_UMD_JS = '/luckysheet/luckysheet.umd.js'

function loadScriptOnce(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const marker = 'data-luckysheet-src'
    let el = document.querySelector(`script[${marker}="${src}"]`) as HTMLScriptElement | null
    if (el) {
      if (el.dataset.loaded === 'true') {
        resolve()
        return
      }
      el.addEventListener('load', () => resolve())
      el.addEventListener('error', () => reject(new Error('Luckysheet 脚本加载失败: ' + src)))
      return
    }
    el = document.createElement('script')
    el.src = src
    el.setAttribute(marker, src)
    el.onload = () => {
      el!.dataset.loaded = 'true'
      resolve()
    }
    el.onerror = () => reject(new Error('Luckysheet 脚本加载失败: ' + src))
    document.head.appendChild(el)
  })
}

/** 确保 Luckysheet API 可用:plugin.js(内含 jQuery)先于主库执行 */
export async function loadLuckysheet(): Promise<void> {
  await loadScriptOnce(LUCKYSHEET_PLUGIN_JS)
  await loadScriptOnce(LUCKYSHEET_UMD_JS)
  if (typeof (window as any).luckysheet?.create !== 'function') {
    throw new Error('Luckysheet 库加载失败')
  }
}
