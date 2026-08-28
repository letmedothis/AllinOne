import router from '@/router'
import cache from '@/plugins/cache'
import { ElMessageBox } from 'element-plus'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isHttp, isEmpty } from "@/utils/validate"
import useLockStore from '@/store/modules/lock'
import defAva from '@/assets/images/profile.jpg'

interface UserState {
  token: string | undefined
  id: string | number
  name: string
  nickName: string
  avatar: string
  roles: string[]
  permissions: string[]
}

const useUserStore = defineStore(
  'user',
  {
    state: (): UserState => ({
      token: getToken(),
      id: '',
      name: '',
      nickName: '',
      avatar: '',
      roles: [],
      permissions: []
    }),
    actions: {
      // 登录
      async login(userInfo: { username: string; password: string; code: string; uuid: string }) {
        const username = userInfo.username.trim()
        const password = userInfo.password
        const code = userInfo.code
        const uuid = userInfo.uuid
        const res = await login(username, password, code, uuid)
        setToken(res.token)
        this.token = res.token
        useLockStore().unlockScreen()
      },
      // 获取用户信息
      async getInfo() {
        const res = await getInfo()
        const user = res.user
        let avatar = user.avatar || ''
        if (!isHttp(avatar)) {
          avatar = (isEmpty(avatar)) ? defAva : import.meta.env.VITE_APP_BASE_API + avatar
        }
        if (res.roles && res.roles.length > 0) {
          this.roles = res.roles
          this.permissions = res.permissions
        } else {
          this.roles = ['ROLE_DEFAULT']
        }
        this.id = user.userId || ''
        this.name = user.userName || ''
        this.nickName = user.nickName || ''
        this.avatar = avatar
        cache.session.set('pwrChrtype', res.pwdChrtype || '')
        /* 初始密码提示 */
        if (res.isDefaultModifyPwd) {
          ElMessageBox.confirm('您的密码还是初始密码，请修改密码！', '安全提示', {
            confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
          }).then(() => {
            router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
          }).catch(() => {})
        }
        /* 过期密码提示 */
        if (!res.isDefaultModifyPwd && res.isPasswordExpired) {
          ElMessageBox.confirm('您的密码已过期，请尽快修改密码！', '安全提示', {
            confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
          }).then(() => {
            router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
          }).catch(() => {})
        }
      },
      // 退出系统
      async logOut() {
        await logout()
        this.token = ''
        this.roles = []
        this.permissions = []
        removeToken()
      }
    }
  })

export default useUserStore
