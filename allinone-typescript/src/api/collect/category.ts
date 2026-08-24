import request from '@/utils/request'
import type { AjaxResult } from '@/types'
import type { CollectCategory } from '@/types/api/collect/category'

/** 查询分类树形列表 */
export function listCategory(): Promise<AjaxResult<CollectCategory[]>> {
  return request({
    url: '/collect/category/list',
    method: 'get'
  })
}

/** 新增分类 */
export function addCategory(data: CollectCategory): Promise<AjaxResult> {
  return request({
    url: '/collect/category',
    method: 'post',
    data: data
  })
}

/** 修改分类 */
export function updateCategory(data: CollectCategory): Promise<AjaxResult> {
  return request({
    url: '/collect/category',
    method: 'put',
    data: data
  })
}

/** 删除分类 */
export function delCategory(id: number): Promise<AjaxResult> {
  return request({
    url: '/collect/category/' + id,
    method: 'delete'
  })
}
