import request from '@/utils/request'
import type { AjaxResult } from '@/types'
import type { ReportCategoryQueryParams, ReportCategory } from '@/types/api/report/category'

/** 查询报表分类列表 */
export function listCategory(query?: ReportCategoryQueryParams): Promise<AjaxResult<ReportCategory[]>> {
  return request({
    url: '/report/category/list',
    method: 'get',
    params: query
  })
}

/** 新增报表分类 */
export function addCategory(data: ReportCategory): Promise<AjaxResult<ReportCategory>> {
  return request({
    url: '/report/category',
    method: 'post',
    data: data
  })
}

/** 修改报表分类 */
export function updateCategory(data: ReportCategory): Promise<AjaxResult> {
  return request({
    url: '/report/category',
    method: 'put',
    data: data
  })
}

/** 删除报表分类 */
export function delCategory(id: number): Promise<AjaxResult> {
  return request({
    url: '/report/category/' + id,
    method: 'delete'
  })
}
