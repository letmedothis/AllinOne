export interface Supplier {
  documentId?: string
  number?: string
  name?: string
  taxId?: string
  contact?: string
  phone?: string
  address?: string
  bankName?: string
  accountName?: string
  bankAccount?: string
  attachmentPaths?: string
  approvalStatus?: string
  currentNode?: string
  revision?: number
  creationKey?: string
  remark?: string
}

export interface PurchaseOrderLine {
  id?: string
  name?: string
  specification?: string
  unit?: string
  quantity?: number
  price?: number
  rate?: number
}

export interface PurchaseOrder {
  documentId?: string
  number?: string
  supplierId?: string
  supplierName?: string
  buyerId?: string
  expectedDate?: string
  approvalStatus?: string
  currentNode?: string
  revision?: number
  totalCents?: number
  lines?: PurchaseOrderLine[]
}

export interface ApprovalTask {
  taskId: string
  instanceId: string
  documentId: string
  versionId: string
  documentType: string
  documentNumber: string
  node: string
  submitterName?: string
  versionNo: number
  revision: number
  documentRevision: number
  submittedAt?: string
}

export interface SupplierOption {
  documentId: string
  name: string
  taxId?: string
}

export interface WorkflowProcessDefinition {
  id: string
  key: string
  name?: string
  version: number
  deploymentId?: string
  resourceName?: string
  suspended?: boolean
}

export interface WorkflowTask {
  id: string
  name?: string
  taskDefinitionKey?: string
  processInstanceId: string
  processDefinitionId: string
  documentId?: string
  documentNumber?: string
  createTime?: string
}

export type WorkflowAssigneeType = 'SUPERVISOR_CONFIG' | 'USER' | 'ROLE' | 'DEPT_LEADER' | 'INITIATOR_MANAGER'

export interface WorkflowCandidateOption {
  type: WorkflowAssigneeType
  value: string
  label: string
  description?: string
}

export interface WorkflowDesign {
  id?: string
  processKey: string
  name: string
  bpmnXml: string
  status?: 'DRAFT' | 'PUBLISHED'
  revision: number
  publishedDefinitionId?: string
  publishedVersion?: number
  updateBy?: string
  updateTime?: string
  publishedAt?: string
}

export interface WorkflowTimelineItem {
  taskId: string
  nodeId?: string
  nodeName?: string
  assigneeId?: string
  assigneeName?: string
  status: 'PENDING' | 'COMPLETED'
  action?: 'APPROVE' | 'REJECT'
  comment?: string
  startTime?: string
  endTime?: string
}

export interface WorkflowInstanceDetail {
  id: string
  processDefinitionId: string
  processDefinitionName?: string
  processDefinitionVersion?: number
  documentId?: string
  documentNumber?: string
  startedBy?: string
  startedByName?: string
  status: 'RUNNING' | 'COMPLETED'
  startTime?: string
  endTime?: string
  timeline?: WorkflowTimelineItem[]
}

export interface WorkflowDeploymentRequest {
  processKey: string
  deploymentName?: string
  bpmnXml: string
}

export interface InvoiceLine { id?: string; orderLineId?: string; name?: string; unit?: string; quantity?: number; price?: number; rate?: number }
export interface Invoice { number?: string; creatorId?: string; approvalStatus?: string; currentNode?: string; documentId?: string; orderId?: string; invoiceNumber?: string; invoiceType?: string; issueDate?: string; sellerName?: string; sellerTaxId?: string; buyerName?: string; buyerTaxId?: string; amountCents?: number; taxCents?: number; totalCents?: number; differenceNote?: string; manualConfirmed?: boolean; revision?: number; lines?: InvoiceLine[] }
export interface OpeningPayable { documentId?: string; revision?: number; supplierId?: string; invoiceNumber?: string; issueDate?: string; openingDate?: string; balanceCents?: number; reason?: string }

export interface PaymentLine { id?: string; invoiceId?: string; invoiceNumber?: string; invoiceTotalCents?: number; allocatedCents?: number; allocatedYuan?: number }
export interface PaymentEvent { id: string; actorName: string; action: string; comment?: string; snapshot: string; createdAt: string }
export interface PaymentExecution { revision: number; paidAt: string; reference: string; payerAccount: string }
export interface Payment {
  id?: string
  number?: string
  supplierId?: string
  supplierName?: string
  amountCents?: number
  thresholdExceeded?: string
  status?: string
  currentNode?: string
  creatorId?: string
  remark?: string
  reviewComment?: string
  directorComment?: string
  revision?: number
  lines?: PaymentLine[]
  events?: PaymentEvent[]
}
export interface ApBalanceRow { invoiceId?: string; invoiceNumber?: string; issueDate?: string; totalCents?: number; allocatedCents?: number; balanceCents?: number; paidCents?: number; reservedCents?: number; unpaidCents?: number }
