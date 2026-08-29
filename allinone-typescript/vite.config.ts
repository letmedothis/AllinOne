import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import fs from 'fs'
import type { Plugin } from 'vite'
import createVitePlugins from './vite/plugins'

const baseUrl = 'http://localhost:8080' // 后端接口

// Luckysheet 本地构建产物（allinone-luckysheet/dist，构建产物不入库）。
// 主库以经典脚本挂到 /luckysheet/ 固定路径下加载：
// 1) 其内部按相对路径动态加载 expendPlugins/chart/*，相对路径必须基于稳定站点路径；
// 2) CSS 中的字体/图片等静态资源同样依赖该路径。
const luckysheetDist = path.resolve(__dirname, '../allinone-luckysheet/dist')

const LUCKYSHEET_MIME: Record<string, string> = {
  '.js': 'text/javascript', '.css': 'text/css', '.html': 'text/html',
  '.json': 'application/json', '.map': 'application/json',
  '.png': 'image/png', '.jpg': 'image/jpeg', '.gif': 'image/gif', '.svg': 'image/svg+xml',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf', '.eot': 'application/vnd.ms-fontobject'
}

function luckysheetStatic(): Plugin {
  return {
    name: 'luckysheet-static-assets',
    configureServer(server) {
      server.middlewares.use('/luckysheet', (req, res) => {
        const rel = decodeURIComponent((req.url || '').split('?')[0])
        const fp = path.join(luckysheetDist, rel)
        if (!fp.startsWith(luckysheetDist) || !fs.existsSync(fp) || fs.statSync(fp).isDirectory()) {
          res.statusCode = 404
          res.end('not found')
          return
        }
        const ext = path.extname(fp).toLowerCase()
        res.setHeader('Content-Type', (LUCKYSHEET_MIME[ext] || 'application/octet-stream') + '; charset=utf-8')
        fs.createReadStream(fp).pipe(res)
      })
    },
    // 生产构建后随产物发布，保证线上路径一致
    closeBundle() {
      const dest = path.resolve(__dirname, 'dist/luckysheet')
      if (fs.existsSync(dest)) fs.rmSync(dest, { recursive: true, force: true })
      fs.cpSync(luckysheetDist, dest, { recursive: true })
      console.log('[luckysheet-static-assets] copied dist ->', dest)
    }
  }
}

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  const { VITE_APP_ENV } = env
  return {
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
    base: VITE_APP_ENV === 'production' ? '/' : '/',
    plugins: [luckysheetStatic(), ...createVitePlugins(env, command === 'build')],
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置路径
        '~': path.resolve(__dirname, './'),
        // 设置别名
        '@': path.resolve(__dirname, './src')
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    // 打包配置
    build: {
      // https://vite.dev/config/build-options.html
      sourcemap: command === 'build' ? false : 'inline',
      outDir: 'dist',
      assetsDir: 'assets',
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
        }
      }
    },
    // vite 相关配置
    server: {
      port: 80,
      host: true,
      open: true,
      fs: {
        // Luckysheet 本地构建产物位于工作区外（allinone-luckysheet/dist），
        // 其 CSS 中的字体/图片等静态资源走 @fs 静态服务，需显式放行，否则 403
        allow: [path.resolve(__dirname), path.resolve(__dirname, '../allinone-luckysheet')]
      },
      proxy: {
        // https://cn.vitejs.dev/config/#server-proxy
        '/dev-api': {
          target: baseUrl,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/dev-api/, '')
        },
         // springdoc proxy
         '^/v3/api-docs/(.*)': {
          target: baseUrl,
          changeOrigin: true,
        },
        // JimuReport / JimuBI 引擎页面与接口（iframe 内嵌，浏览器直接导航，无法走 /dev-api 前缀）
        '/jmreport': {
          target: baseUrl,
          changeOrigin: true
        },
        '/jimubi': {
          target: baseUrl,
          changeOrigin: true
        },
        '/drag': {
          target: baseUrl,
          changeOrigin: true
        }
      }
    },
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: (atRule: any) => {
                if (atRule.name === 'charset') {
                  atRule.remove()
                }
              }
            }
          }
        ]
      }
    }
  }
})

