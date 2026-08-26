import type { BaseEntity } from '../common'

/** 报表分类查询参数 */
export interface ReportCategoryQueryParams {
  categoryName?: string
  status?: string
}

/** 报表分类信息 */
export interface ReportCategory extends BaseEntity {
  categoryId?: number
  /** 父分类ID */
  parentId?: number
  categoryName?: string
  /** 显示顺序 */
  orderNum?: number
  /** 状态（0正常 1停用） */
  status?: '0' | '1'
  /** 子分类 */
  children?: ReportCategory[]
}
