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

export interface InvoiceLine { id?: string; orderLineId?: string; name?: string; unit?: string; quantity?: number; price?: number; rate?: number }
export interface Invoice { documentId?: string; orderId?: string; invoiceNumber?: string; invoiceType?: string; issueDate?: string; sellerName?: string; sellerTaxId?: string; buyerName?: string; buyerTaxId?: string; amountCents?: number; taxCents?: number; totalCents?: number; differenceNote?: string; manualConfirmed?: boolean; revision?: number; lines?: InvoiceLine[] }
