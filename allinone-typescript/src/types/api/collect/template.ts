import type { PageDomain, BaseEntity } from '../common'

/** 填报模板查询参数 */
export interface CollectTemplateQueryParams extends PageDomain {
  templateName?: string
  templateCode?: string
  /** 模板状态（0草稿 1已发布 2已下架） */
  status?: string
  /** 分类ID */
  categoryId?: number
  categoryName?: string
}

/** 填报模板信息 */
export interface CollectTemplate extends BaseEntity {
  templateId?: number
  templateName?: string
  templateCode?: string
  categoryId?: number
  templateType?: string
  templateJson?: string
  status?: '0' | '1' | '2'
  version?: number
}
