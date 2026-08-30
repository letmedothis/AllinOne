import type { PageDomain, BaseEntity } from '../common'

/** 填报数据查询参数，与后端 CollectData 字段保持一致。 */
export interface CollectDataQueryParams extends PageDomain {
  templateId?: number
  templateName?: string
  bizStatus?: 'draft' | 'submitted'
  createBy?: string
}

/** 填报数据信息，与后端 CollectData 字段保持一致。 */
export interface CollectData extends BaseEntity {
  dataId?: number
  templateId?: number
  templateName?: string
  templateCode?: string
  formData?: string
  bizStatus?: 'draft' | 'submitted'
  deptId?: number
  dataCode?: string
  version?: number
  /** 填报/提交时所用模板版本快照 */
  templateVersion?: number
  submitBy?: string
  submitTime?: string
}

/** 填报提交参数 */
export interface CollectDataSubmitParams {
  dataId?: number
  formData?: string
  version?: number
}
