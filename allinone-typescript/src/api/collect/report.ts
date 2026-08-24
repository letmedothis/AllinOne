import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, ReportQueryParams, WorkReport } from '@/types'

export function listReport(query: ReportQueryParams): Promise<TableDataInfo<WorkReport[]>> {
  return request({ url: '/collect/report/list', method: 'get', params: query })
}

export function getReport(id: string): Promise<AjaxResult<WorkReport>> {
  return request({ url: '/collect/report/' + id, method: 'get' })
}

export function addReport(data: WorkReport): Promise<AjaxResult> {
  return request({ url: '/collect/report', method: 'post', data: data })
}

export function updateReport(data: WorkReport): Promise<AjaxResult> {
  return request({ url: '/collect/report', method: 'put', data: data })
}

export function delReport(id: string | string[]): Promise<AjaxResult> {
  return request({ url: '/collect/report/' + id, method: 'delete' })
}

export interface SheetIdMapping {
  clientSheetId: string
  sheetDbId: string
}

export function saveSheet(id: string, data: any, deletedSheetIds: string[] = []): Promise<AjaxResult<SheetIdMapping[]>> {
  return request({ url: '/collect/report/sheet/' + id, method: 'put', data: { data, deletedSheetIds } })
}

export function getSheet(id: string): Promise<AjaxResult<any[]>> {
  return request({ url: '/collect/report/sheet/' + id, method: 'get' })
}

export function loadCells(sheetDbId: string, startRow: number, endRow: number, startCol: number, endCol: number): Promise<AjaxResult<any[]>> {
  return request({ url: '/collect/report/cells', method: 'get', params: { sheetDbId, startRow, endRow, startCol, endCol } })
}

export function saveCells(cells: any[]): Promise<AjaxResult> {
  return request({ url: '/collect/report/cells', method: 'put', data: { cells } })
}

export function getPermissions(sheetDbId: string): Promise<AjaxResult<any[]>> {
  return request({ url: '/collect/report/permissions/' + sheetDbId, method: 'get' })
}

export function grantPermission(sheetDbId: string, permType: string, permId: number): Promise<AjaxResult> {
  return request({ url: '/collect/report/permissions/' + sheetDbId, method: 'post', data: { permType, permId } })
}

export function revokePermission(sheetDbId: string, permType: string, permId: number): Promise<AjaxResult> {
  return request({ url: '/collect/report/permissions/' + sheetDbId, method: 'delete', params: { permType, permId } })
}
