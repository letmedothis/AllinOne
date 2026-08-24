import type { PageDomain, BaseEntity } from '../common'

/** 填报模板查询参数 */
export interface CollectTemplateQueryParams extends PageDomain {
  /** 模板名称 */
  name?: string
  /** 模板状态（0草稿 1已发布 2已下架） */
  status?: string
  /** 分类ID */
  categoryId?: number
}

/** 填报模板信息 */
export interface CollectTemplate extends BaseEntity {
  /** 模板ID */
  id?: number
  /** 模板名称 */
  name?: string
  /** 模板编码 */
  code?: string
  /** 分类ID */
  categoryId?: number
  /** 分类名称 */
  categoryName?: string
  /** 模板配置（JSON字符串，Luckysheet配置） */
  config?: string
  /** 模板描述 */
  description?: string
  /** 模板状态（0草稿 1已发布 2已下架） */
  status?: '0' | '1' | '2'
  /** 版本号 */
  version?: number
  /** 发布时间 */
  publishTime?: string
}
