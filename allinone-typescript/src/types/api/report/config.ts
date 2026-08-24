import type { PageDomain, BaseEntity } from '../common'

/** 报表配置查询参数 */
export interface ReportConfigQueryParams extends PageDomain {
  /** 报表名称 */
  name?: string
  /** 报表状态 */
  status?: string
}

/** 报表配置信息 */
export interface ReportConfig extends BaseEntity {
  /** 报表ID */
  id?: number
  /** 报表名称 */
  name?: string
  /** 报表编码 */
  code?: string
  /** 报表URL */
  url?: string
  /** 报表类型（1iframe嵌入 2大屏） */
  type?: '1' | '2'
  /** 报表状态（0正常 1停用） */
  status?: '0' | '1'
  /** 报表描述 */
  description?: string
  /** 报表配置（JSON） */
  config?: string
  /** 报表缩略图 */
  thumbnail?: string
  /** 报表高度 */
  height?: number
  /** 报表宽度 */
  width?: number
}
