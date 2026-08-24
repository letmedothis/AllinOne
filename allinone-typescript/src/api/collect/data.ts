import request from '@/utils/request'
import type { CollectDataQueryParams, CollectData, AjaxResult, TableDataInfo } from '@/types'

/** 查询填报数据列表 */
export function listData(query: CollectDataQueryParams): Promise<TableDataInfo<CollectData[]>> {
  return request({
    url: '/collect/data/list',
    method: 'get',
    params: query
  })
}

/** 查询填报数据详情 */
export function getData(id: number): Promise<AjaxResult<CollectData>> {
  return request({
    url: '/collect/data/' + id,
    method: 'get'
  })
}

/** 新增或保存草稿 */
export function addData(data: CollectData): Promise<AjaxResult> {
  return request({
    url: '/collect/data',
    method: 'post',
    data: data
  })
}

/** 修改填报数据 */
export function updateData(data: CollectData): Promise<AjaxResult> {
  return request({
    url: '/collect/data',
    method: 'put',
    data: data
  })
}

/** 删除填报数据 */
export function delData(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/collect/data/' + id,
    method: 'delete'
  })
}

/** 提交填报数据 */
export function submitData(id: number): Promise<AjaxResult> {
  return request({
    url: '/collect/data/' + id + '/submit',
    method: 'post'
  })
}
