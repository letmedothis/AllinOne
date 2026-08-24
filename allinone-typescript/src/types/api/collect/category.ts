import type { BaseEntity } from '../common'

/** 分类查询参数 */
export interface CategoryQueryParams {
  /** 分类名称 */
  name?: string
}

/** 分类信息 */
export interface CollectCategory extends BaseEntity {
  /** 分类ID */
  id?: number
  /** 父分类ID */
  parentId?: number
  /** 分类名称 */
  name?: string
  /** 分类编码 */
  code?: string
  /** 显示顺序 */
  orderNum?: number
  /** 状态（0正常 1停用） */
  status?: '0' | '1'
  /** 子分类 */
  children?: CollectCategory[]
}
