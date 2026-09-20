 /**
 * v-hasPermi 操作权限处理
 * Copyright (c) 2019 ruoyi
 */
import useUserStore from '@/store/modules/user'

export default {
  mounted(el: HTMLElement, binding: DirectiveBinding, vnode: any) {
    const { value } = binding
    const all_permission = "*:*:*"
    const permissions = useUserStore().permissions

    if (value && value instanceof Array && value.length > 0) {
      const hasPermissions = permissions.some((permission: string) => {
        return all_permission === permission || value.some(p => p === permission)
      })

      if (!hasPermissions) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error(`请设置操作权限标签值`)
    }
  }
}
