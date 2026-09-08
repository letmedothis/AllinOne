import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type { ApprovalTask, Invoice, OpeningPayable, PurchaseOrder, Supplier, SupplierOption, ApBalanceRow, Payment, WorkflowCandidateOption, WorkflowDeploymentRequest, WorkflowDesign, WorkflowInstanceDetail, WorkflowProcessDefinition, WorkflowTask } from '@/types/api/supply'

export function listSuppliers(query: Record<string, unknown>): Promise<TableDataInfo<Supplier[]>> { return request({ url: '/supply/suppliers/list', method: 'get', params: query }) }
export function getSupplier(id: string): Promise<AjaxResult<Supplier>> { return request({ url: `/supply/suppliers/${id}`, method: 'get' }) }
export function addSupplier(data: Supplier): Promise<AjaxResult> { return request({ url: '/supply/suppliers', method: 'post', data }) }
export function updateSupplier(data: Supplier): Promise<AjaxResult> { return request({ url: '/supply/suppliers', method: 'put', data }) }
export function submitSupplier(id: string): Promise<AjaxResult> { return request({ url: `/supply/suppliers/${id}/submit`, method: 'post' }) }
export function listOrders(query: Record<string, unknown>): Promise<TableDataInfo<PurchaseOrder[]>> { return request({ url: '/supply/orders/list', method: 'get', params: query }) }
export function listOrderSupplierOptions(): Promise<AjaxResult<SupplierOption[]>> { return request({ url: '/supply/orders/supplier-options', method: 'get' }) }
export function getOrder(id: string): Promise<AjaxResult<PurchaseOrder>> { return request({ url: `/supply/orders/${id}`, method: 'get' }) }
export function addOrder(data: PurchaseOrder): Promise<AjaxResult> { return request({ url: '/supply/orders', method: 'post', data }) }
export function updateOrder(data: PurchaseOrder): Promise<AjaxResult> { return request({ url: '/supply/orders', method: 'put', data }) }
export function submitOrder(id: string): Promise<AjaxResult> { return request({ url: `/supply/orders/${id}/submit`, method: 'post' }) }
export function listPendingApprovals(): Promise<TableDataInfo<ApprovalTask[]>> { return request({ url: '/supply/approvals/pending', method: 'get' }) }
export function approve(data: ApprovalTask & { comment?: string }): Promise<AjaxResult> { return request({ url: '/supply/approvals/approve', method: 'post', data }) }
export function reject(data: ApprovalTask & { comment?: string }): Promise<AjaxResult> { return request({ url: '/supply/approvals/reject', method: 'post', data }) }
export function transferApproval(data: { taskId: string | number; assigneeId: string | number }): Promise<AjaxResult> { return request({ url: '/supply/approvals/transfer', method: 'post', data }) }
export function prepareReceipt(orderId: string): Promise<AjaxResult> { return request({ url: `/supply/receipts/prepare/${orderId}`, method: 'get' }) }
export function confirmReceipt(data: Record<string, unknown>): Promise<AjaxResult> { return request({ url: '/supply/receipts/confirm', method: 'post', data }) }
export function parseInvoiceXml(xml: string): Promise<AjaxResult> { return request({ url: '/supply/invoices/parse-xml', method: 'post', data: xml, headers: { 'Content-Type': 'text/plain' } }) }
export function addInvoice(data: Invoice): Promise<AjaxResult<Invoice>> { return request({ url: '/supply/invoices', method: 'post', data }) }
export function updateInvoice(data: Invoice): Promise<AjaxResult> { return request({ url: '/supply/invoices', method: 'put', data }) }
export function confirmInvoice(data: Invoice): Promise<AjaxResult> { return request({ url: '/supply/invoices/confirm', method: 'post', data }) }
export function submitInvoice(data: Invoice): Promise<AjaxResult> { return request({ url: '/supply/invoices/submit', method: 'post', data }) }
export function listInvoices(query: Record<string, unknown>): Promise<TableDataInfo<Invoice[]>> { return request({ url: '/supply/invoices/list', method: 'get', params: query }) }
export function getInvoice(id: string): Promise<AjaxResult<Invoice>> { return request({ url: `/supply/invoices/${id}`, method: 'get' }) }
export function invoiceOrderOptions(): Promise<AjaxResult<PurchaseOrder[]>> { return request({ url: '/supply/invoices/order-options', method: 'get' }) }
export function invoiceOrder(id: string): Promise<AjaxResult<PurchaseOrder>> { return request({ url: `/supply/invoices/orders/${id}`, method: 'get' }) }
export function listCombinedLedger(query: Record<string, unknown>): Promise<TableDataInfo<any[]>> { return request({ url: '/supply/ledger/combined', method: 'get', params: query }) }
export function exportCombinedLedger(query: Record<string, unknown>) { return request({ url: '/supply/ledger/export', method: 'post', params: query, responseType: 'blob' }) }
export function listDocumentAttachments(documentId: string | number) { return request({ url: `/supply/documents/${documentId}/attachments`, method: 'get' }) }
export function uploadDocumentAttachment(documentId: string | number, file: File, attachmentType = 'SUPPORTING') { const data = new FormData(); data.append('file', file); data.append('attachmentType', attachmentType); return request({ url: `/supply/documents/${documentId}/attachments`, method: 'post', data }) }
export function removeDocumentAttachment(documentId: string | number, fileId: string | number) { return request({ url: `/supply/documents/${documentId}/attachments/${fileId}`, method: 'delete' }) }
export function listDocumentComments(documentId: string | number) { return request({ url: `/supply/documents/${documentId}/comments`, method: 'get' }) }
export function addDocumentComment(documentId: string | number, content: string) { return request({ url: `/supply/documents/${documentId}/comments`, method: 'post', data: { content } }) }
export function withdrawDocument(documentId: string | number, reason: string) { return request({ url: `/supply/documents/${documentId}/withdraw`, method: 'post', data: { reason } }) }
export function voidDocument(documentId: string | number, reason: string) { return request({ url: `/supply/documents/${documentId}/void`, method: 'post', data: { reason } }) }
export function restoreInvoice(documentId: string | number, reason: string) { return request({ url: `/supply/documents/${documentId}/restore`, method: 'post', data: { reason } }) }
export function deleteDraftDocument(documentId: string | number) { return request({ url: `/supply/documents/${documentId}`, method: 'delete' }) }
export function listWorkflowConfigs(): Promise<AjaxResult<any[]>> { return request({ url: '/supply/config/workflows', method: 'get' }) }
export function saveWorkflowConfig(data: Record<string, unknown>): Promise<AjaxResult> { return request({ url: '/supply/config/workflows', method: 'put', data }) }
export function listWorkflowDefinitions(): Promise<AjaxResult<WorkflowProcessDefinition[]>> { return request({ url: '/supply/workflow/definitions', method: 'get' }) }
export function deployWorkflowDefinition(data: WorkflowDeploymentRequest): Promise<AjaxResult<WorkflowProcessDefinition>> { return request({ url: '/supply/workflow/definitions/deploy', method: 'post', data }) }
export function getWorkflowDefinitionXml(id: string): Promise<AjaxResult<string>> { return request({ url: `/supply/workflow/definitions/${encodeURIComponent(id)}/xml`, method: 'get' }) }
export function changeWorkflowDefinitionState(id: string, suspended: boolean): Promise<AjaxResult> { return request({ url: `/supply/workflow/definitions/${encodeURIComponent(id)}/state`, method: 'put', data: { suspended } }) }
export function listWorkflowCandidates(): Promise<AjaxResult<WorkflowCandidateOption[]>> { return request({ url: '/supply/workflow/candidates', method: 'get' }) }
export function getWorkflowDesign(processKey: string): Promise<AjaxResult<WorkflowDesign>> { return request({ url: `/supply/workflow/designs/${processKey}`, method: 'get' }) }
export function saveWorkflowDraft(data: WorkflowDesign): Promise<AjaxResult<WorkflowDesign>> { return request({ url: '/supply/workflow/designs/save', method: 'post', data }) }
export function publishWorkflowDesign(data: WorkflowDesign): Promise<AjaxResult<WorkflowProcessDefinition>> { return request({ url: '/supply/workflow/designs/publish', method: 'post', data }) }
export function listMyWorkflowTasks(): Promise<AjaxResult<WorkflowTask[]>> { return request({ url: '/supply/workflow/tasks/mine', method: 'get' }) }
export function completeWorkflowTask(taskId: string, data: { action: 'APPROVE' | 'REJECT'; comment?: string }): Promise<AjaxResult> { return request({ url: `/supply/workflow/tasks/${taskId}/complete`, method: 'post', data }) }
export function transferWorkflowTask(taskId: string, assigneeId: string | number): Promise<AjaxResult> { return request({ url: `/supply/workflow/tasks/${taskId}/transfer`, method: 'post', data: { taskId, assigneeId } }) }
export function listMyStartedWorkflowInstances(): Promise<AjaxResult<WorkflowInstanceDetail[]>> { return request({ url: '/supply/workflow/instances/mine', method: 'get' }) }
export function getWorkflowInstanceDetail(instanceId: string): Promise<AjaxResult<WorkflowInstanceDetail>> { return request({ url: `/supply/workflow/instances/${encodeURIComponent(instanceId)}`, method: 'get' }) }
export function listPayments(query: Record<string, unknown>): Promise<TableDataInfo<Payment[]>> { return request({ url: '/supply/payments/list', method: 'get', params: query }) }
export function getPayment(id: string | number): Promise<AjaxResult<Payment>> { return request({ url: `/supply/payments/${id}`, method: 'get' }) }
export function listPaymentSupplierOptions(): Promise<AjaxResult<SupplierOption[]>> { return request({ url: '/supply/payments/supplier-options', method: 'get' }) }
export function addPayment(data: Payment): Promise<AjaxResult<Payment>> { return request({ url: '/supply/payments', method: 'post', data }) }
export function updatePayment(data: Payment): Promise<AjaxResult> { return request({ url: '/supply/payments', method: 'put', data }) }
export function submitPayment(id: string | number): Promise<AjaxResult> { return request({ url: `/supply/payments/${id}/submit`, method: 'post' }) }
export function reviewPayment(id: string | number, data: { approved: boolean; comment?: string; revision: number }): Promise<AjaxResult> { return request({ url: `/supply/payments/${id}/review`, method: 'post', data }) }
export function directorDecisionPayment(id: string | number, data: { approved: boolean; comment?: string; revision: number }): Promise<AjaxResult> { return request({ url: `/supply/payments/${id}/director-decision`, method: 'post', data }) }
export function recordPaymentExecution(id: string, data: import('@/types/api/supply').PaymentExecution): Promise<AjaxResult> { return request({ url: `/supply/payments/${id}/execution`, method: 'post', data }) }
export function voidPayment(id: string | number, reason?: string): Promise<AjaxResult> { return request({ url: `/supply/payments/${id}/void`, method: 'post', params: { reason } }) }
export function listApBalance(supplierId?: string | number): Promise<AjaxResult<ApBalanceRow[]>> { return request({ url: '/supply/payments/ap-balance', method: 'get', params: { supplierId } }) }
export function openingSupplierOptions(): Promise<AjaxResult<SupplierOption[]>> { return request({ url: '/supply/opening-payables/supplier-options', method: 'get' }) }
export function listOpeningPayables(): Promise<AjaxResult<OpeningPayable[]>> { return request({ url: '/supply/opening-payables/list', method: 'get' }) }
export function getOpeningPayable(id: string | number): Promise<AjaxResult<OpeningPayable>> { return request({ url: `/supply/opening-payables/${id}`, method: 'get' }) }
export function saveOpeningPayable(data: OpeningPayable): Promise<AjaxResult<Invoice>> { return request({ url: '/supply/opening-payables', method: 'post', data }) }
export function submitOpeningPayable(id: string | number, revision: number): Promise<AjaxResult> { return request({ url: `/supply/opening-payables/${id}/submit`, method: 'post', params: { revision } }) }
