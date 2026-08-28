// CSS Module type declarations
declare module '*.module.scss' {
  const classes: { readonly [key: string]: string }
  export default classes
}

declare module '*.module.sass' {
  const classes: { readonly [key: string]: string }
  export default classes
}

declare module '*.module.css' {
  const classes: { readonly [key: string]: string }
  export default classes
}

declare module '*.module.less' {
  const classes: { readonly [key: string]: string }
  export default classes
}

// Specific declaration for variables.module.scss
declare module '@/assets/styles/variables.module.scss' {
  const variables: {
    menuText: string;
    menuActiveText: string;
    menuBg: string;
    menuHover: string;
    menuLightBg: string;
    menuLightHover: string;
    menuLightText: string;
    menuLightActiveText: string;
    sideBarWidth: string;
    blue: string;
    lightBlue: string;
    red: string;
    pink: string;
    green: string;
    tiffany: string;
    yellow: string;
    panGreen: string;
    colorPrimary: string;
    colorSuccess: string;
    colorWarning: string;
    colorDanger: string;
    colorInfo: string;
  }
  export default variables
}