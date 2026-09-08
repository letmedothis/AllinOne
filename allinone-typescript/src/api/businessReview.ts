import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface ReviewCase { id?: string; type: string; targetId?: string; ownerId?: string; deptId?: string; title?: string; status?: string; node?: string; revision?: number; baseVersion?: number; beforeJson?: string; afterJson?: string; reason?: string; sensitive?: boolean; supervisorId?: string; financeId?: string }
export function listReview(type: string): Promise<TableDataInfo<ReviewCase[]>> { return request({ url: '/business/reviews', method: 'get', params: { type } }) }
export function saveReview(data: ReviewCase): Promise<AjaxResult<ReviewCase>> { return request({ url: '/business/reviews', method: 'post', data }) }
export function submitReview(id: string, data: { revision:number; key:string }): Promise<AjaxResult<ReviewCase>> { return request({ url: `/business/reviews/${id}/submit`, method: 'post', data }) }
export function decideReview(id: string, data: { revision:number; key:string; comment:string; approved:boolean }): Promise<AjaxResult<ReviewCase>> { return request({ url: `/business/reviews/${id}/decide`, method: 'post', data }) }
export function cancelReview(id: string, data: { revision:number; key:string; comment:string }): Promise<AjaxResult<ReviewCase>> { return request({ url: `/business/reviews/${id}/cancel`, method: 'post', data }) }
export function getReview(id: string): Promise<AjaxResult<{ request: ReviewCase; events: any[]; files: any[] }>> { return request({ url: `/business/reviews/${id}`, method: 'get' }) }
