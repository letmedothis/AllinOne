import request from '@/utils/request'
import type { CollectTemplateQueryParams, CollectTemplate, AjaxResult, TableDataInfo } from '@/types'

/** 查询填报模板列表 */
export function listTemplate(query: CollectTemplateQueryParams): Promise<TableDataInfo<CollectTemplate[]>> {
  return request({
    url: '/collect/template/list',
    method: 'get',
    params: query
  })
}

/** 查询填报模板详情 */
export function getTemplate(id: number): Promise<AjaxResult<CollectTemplate>> {
  return request({
    url: '/collect/template/' + id,
    method: 'get'
  })
}

/** 新增填报模板 */
export function addTemplate(data: CollectTemplate): Promise<AjaxResult<CollectTemplate>> {
  return request({
    url: '/collect/template',
    method: 'post',
    data: data
  })
}

/** 修改填报模板 */
export function updateTemplate(data: CollectTemplate): Promise<AjaxResult<CollectTemplate>> {
  return request({
    url: '/collect/template',
    method: 'put',
    data: data
  })
}

/** 删除填报模板 */
export function delTemplate(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/collect/template/' + id,
    method: 'delete'
  })
}

/** 发布/下架填报模板 */
export function publishTemplate(id: number, status: '1' | '2'): Promise<AjaxResult> {
  return request({
    url: '/collect/template/' + id + '/publish',
    method: 'post',
    data: { status }
  })
}
