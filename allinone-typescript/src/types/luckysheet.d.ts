// Luckysheet UMD module declaration
declare module 'luckysheet' {
  interface LuckysheetOptions {
    container: string;
    title?: string;
    lang?: string;
    allowUpdate?: boolean;
    showtoolbar?: boolean;
    showinfobar?: boolean;
    showsheetbar?: boolean;
    showstatisticBar?: boolean;
    sheetFormulaBar?: boolean;
    allowCopy?: boolean;
    myFolderUrl?: string;
    data?: any[];
    hook?: {
      cellUpdated?: (cell: any, r: number, c: number) => void;
      [key: string]: any;
    };
    [key: string]: any;
  }

  interface LuckysheetStatic {
    create(options: LuckysheetOptions): void;
    getAllSheets(): any[];
    getluckysheetfile(): any[];
    destroy(): void;
    [key: string]: any;
  }

  const luckysheet: LuckysheetStatic;
  export default luckysheet;
}

declare module 'luckysheet/dist/plugins/js/plugin.js' {
  const plugin: any;
  export default plugin;
}

declare module 'luckysheet/dist/plugins/css/pluginsCss.css' {
  const css: string;
  export default css;
}

declare module 'luckysheet/dist/plugins/plugins.css' {
  const css: string;
  export default css;
}

declare module 'luckysheet/dist/css/luckysheet.css' {
  const css: string;
  export default css;
}

// Global window extension
interface Window {
  luckysheet: any;
}