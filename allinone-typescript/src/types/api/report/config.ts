import type { PageDomain, BaseEntity } from '../common'

/** 报表配置查询参数 */
export interface ReportConfigQueryParams extends PageDomain {
  /** 报表名称 */
  reportName?: string
  /** 报表类型（0报表 1大屏 2仪表盘） */
  reportType?: string
  /** 报表状态（0正常 1停用） */
  status?: string
  /** 分类ID */
  categoryId?: number
}

/** 报表配置信息（与后端 report_config 表对齐） */
export interface ReportConfig extends BaseEntity {
  /** 报表ID */
  reportId?: number
  /** 报表名称 */
  reportName?: string
  /** 报表编码（唯一） */
  reportCode?: string
  /** 报表类型（0报表 1大屏 2仪表盘） */
  reportType?: '0' | '1' | '2'
  /** JimuReport 报表ID（type=0 时使用） */
  jimuReportId?: string
  /** JimuBI 大屏/仪表盘ID（type=1/2 时使用） */
  jmbiId?: string
  /** 所属分类ID */
  categoryId?: number
  /** 分类名称（后端联表返回） */
  categoryName?: string
  /** 图标 */
  icon?: string
  /** 显示顺序 */
  orderNum?: number
  /** 状态（0正常 1停用） */
  status?: '0' | '1'
  /** 备注 */
  remark?: string
  /** 访问URL（后端按类型计算返回） */
  url?: string
}
