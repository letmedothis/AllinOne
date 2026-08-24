import type { PageDomain, BaseEntity } from '../common'

/** 填报数据查询参数 */
export interface CollectDataQueryParams extends PageDomain {
  /** 模板ID */
  templateId?: number
  /** 模板名称 */
  templateName?: string
  /** 填报状态（0草稿 1已提交） */
  status?: string
  /** 创建人 */
  createBy?: string
}

/** 填报数据信息 */
export interface CollectData extends BaseEntity {
  /** 填报数据ID */
  id?: number
  /** 模板ID */
  templateId?: number
  /** 模板名称 */
  templateName?: string
  /** 模板编码 */
  templateCode?: string
  /** 填报数据（JSON字符串，Luckysheet数据） */
  data?: string
  /** 填报状态（0草稿 1已提交） */
  status?: '0' | '1'
  /** 提交时间 */
  submitTime?: string
  /** 数据标题 */
  title?: string
}

/** 填报提交参数 */
export interface CollectDataSubmitParams {
  /** 填报数据ID */
  id?: number
  /** 填报数据 */
  data?: string
}
