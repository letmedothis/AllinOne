import { seriesLoadScripts, loadLinks } from '../../utils/util'


// 打印插件产物(luckysheetPluginPrint.umd.js/css)未随本仓库构建与分发,
// 系统亦未启用打印插件(plugins 仅含 'chart'),打印/导出统一走积木报表。
// 如需启用:先构建插件产物并在下方填入实际可访问路径,再在
// luckysheet.create 的 plugins 中加入 'print'。
const dependScripts = [
    // 'expendPlugins/print/luckysheetPluginPrint.umd.js',
]

const dependLinks = [
    // 'expendPlugins/print/luckysheetPluginPrint.css',
]

// Initialize the chart component
function print(data, isDemo) {
    loadLinks(dependLinks);

    seriesLoadScripts(dependScripts, null, function () {

    });
}



export { print }
