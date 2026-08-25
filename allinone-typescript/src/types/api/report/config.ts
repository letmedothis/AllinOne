import type { PageDomain, BaseEntity } from '../common'

/** 报表配置查询参数 */
export interface ReportConfigQueryParams extends PageDomain {
  /** 报表名称 */
  reportName?: string
  /** 报表类型（0报表 1大屏 2仪表盘） */
  reportType?: '0' | '1' | '2'
  /** 报表状态 */
  status?: string
}

/** 报表配置信息 */
export interface ReportConfig extends BaseEntity {
  /** 报表ID */
  reportId?: number
  /** 报表名称 */
  reportName?: string
  /** 报表编码 */
  reportCode?: string
  /** 报表类型（0报表 1大屏 2仪表盘） */
  reportType?: '0' | '1' | '2'
  /** JimuReport 报表ID（reportType=0） */
  jimuReportId?: string
  /** JimuBI 大屏/仪表盘ID（reportType=1/2） */
  jmbiId?: string
  /** 所属分类ID */
  categoryId?: number
  /** 图标 */
  icon?: string
  /** 显示顺序 */
  orderNum?: number
  /** 报表状态（0正常 1停用） */
  status?: '0' | '1'
}
