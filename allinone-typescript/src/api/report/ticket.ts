import request from '@/utils/request'
import type { AjaxResult } from '@/types'

/** 申请 JimuReport/JimuBI 一次性票据（需登录态），返回的 ticket 有效期 60 秒且仅可使用一次 */
export function requestJimuTicket(): Promise<AjaxResult<string>> {
  return request({
    url: '/system/jimu/ticket',
    method: 'post'
  })
}
