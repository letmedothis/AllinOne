import request from '@/utils/request'
import type { ReportConfigQueryParams, ReportConfig, JimuReportDefinition, AjaxResult, TableDataInfo } from '@/types'

/** 获取可配置的 JimuReport 报表目录。 */
export function listJimuReports(): Promise<AjaxResult<JimuReportDefinition[]>> {
  return request({
    url: '/report/config/jimu-reports',
    method: 'get'
  })
}

/** 查询报表配置列表 */
export function listConfig(query: ReportConfigQueryParams): Promise<TableDataInfo<ReportConfig[]>> {
  return request({
    url: '/report/config/list',
    method: 'get',
    params: query
  })
}

/** 查询报表配置详情 */
export function getConfig(id: number): Promise<AjaxResult<ReportConfig>> {
  return request({
    url: '/report/config/' + id,
    method: 'get'
  })
}

/** 新增报表配置 */
export function addConfig(data: ReportConfig): Promise<AjaxResult> {
  return request({
    url: '/report/config',
    method: 'post',
    data: data
  })
}

/** 修改报表配置 */
export function updateConfig(data: ReportConfig): Promise<AjaxResult> {
  return request({
    url: '/report/config',
    method: 'put',
    data: data
  })
}

/** 删除报表配置 */
export function delConfig(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/report/config/' + id,
    method: 'delete'
  })
}
