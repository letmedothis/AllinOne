import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, PageDomain, BaseEntity } from '@/types'

/** 字段映射查询参数 */
export interface FieldMappingQueryParams extends PageDomain {
  /** 关联模板ID */
  templateId?: number
  /** 目标表名 */
  targetTable?: string
}

/** 字段映射信息（collect_field_mapping，Tier 3 数据回写配置） */
export interface CollectFieldMapping extends BaseEntity {
  mappingId?: number
  /** 关联模板ID */
  templateId?: number
  /** 单元格坐标（如B3） */
  cellRef?: string
  /** Sheet序号（0-based） */
  sheetIndex?: number
  /** 行号（0-based） */
  rowIndex?: number
  /** 列号（0-based） */
  colIndex?: number
  /** 目标表名 */
  targetTable?: string
  /** 目标列名 */
  targetColumn?: string
  /** 数据类型 */
  dataType?: string
  /** 主键顺序（0非主键） */
  pkOrder?: number
  /** 默认值 */
  defaultValue?: string
  /** 转换类型（0无 1格式化 2脚本 3Bean） */
  transformType?: string
  /** 转换脚本 */
  transformScript?: string
  /** 处理顺序 */
  orderNum?: number
}

/** 查询字段映射列表 */
export function listMapping(query?: FieldMappingQueryParams): Promise<TableDataInfo<CollectFieldMapping[]>> {
  return request({
    url: '/collect/mapping/list',
    method: 'get',
    params: query
  })
}

/** 查询字段映射详情 */
export function getMapping(mappingId: number): Promise<AjaxResult<CollectFieldMapping>> {
  return request({
    url: '/collect/mapping/' + mappingId,
    method: 'get'
  })
}

/** 新增字段映射 */
export function addMapping(data: CollectFieldMapping): Promise<AjaxResult<CollectFieldMapping>> {
  return request({
    url: '/collect/mapping',
    method: 'post',
    data: data
  })
}

/** 修改字段映射 */
export function updateMapping(data: CollectFieldMapping): Promise<AjaxResult> {
  return request({
    url: '/collect/mapping',
    method: 'put',
    data: data
  })
}

/** 删除字段映射 */
export function delMapping(mappingIds: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/collect/mapping/' + mappingIds,
    method: 'delete'
  })
}
