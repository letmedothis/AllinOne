import type { PageDomain, BaseEntity } from '../common'

/** 报表查询参数 */
export interface ReportQueryParams extends PageDomain {
  reportName?: string
  reportJianjie?: string
  /** 创建者ID */
  userId?: number
  /** 部门ID */
  deptId?: number
}

/** 报表（work_report） */
export interface WorkReport extends BaseEntity {
  id?: string
  /** 报表名 */
  reportName?: string
  /** 报表简介 */
  reportJianjie?: string
  /** 备注 */
  reportBeizhu?: string
  /** 创建者ID */
  userId?: number
  /** 部门ID */
  deptId?: number
  /** 逻辑删除状态 0未删除 1已删除 */
  delStatus?: number
}
