import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type { ApprovalTask, Invoice, PurchaseOrder, Supplier, SupplierOption } from '@/types/api/supply'

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
export function prepareReceipt(orderId: string): Promise<AjaxResult> { return request({ url: `/supply/receipts/prepare/${orderId}`, method: 'get' }) }
export function confirmReceipt(data: Record<string, unknown>): Promise<AjaxResult> { return request({ url: '/supply/receipts/confirm', method: 'post', data }) }
export function parseInvoiceXml(xml: string): Promise<AjaxResult> { return request({ url: '/supply/invoices/parse-xml', method: 'post', data: xml, headers: { 'Content-Type': 'text/plain' } }) }
export function addInvoice(data: Invoice): Promise<AjaxResult<Invoice>> { return request({ url: '/supply/invoices', method: 'post', data }) }
export function updateInvoice(data: Invoice): Promise<AjaxResult> { return request({ url: '/supply/invoices', method: 'put', data }) }
export function confirmInvoice(data: Invoice): Promise<AjaxResult> { return request({ url: '/supply/invoices/confirm', method: 'post', data }) }
export function submitInvoice(data: Invoice): Promise<AjaxResult> { return request({ url: '/supply/invoices/submit', method: 'post', data }) }
export function listInvoices(query: Record<string, unknown>): Promise<TableDataInfo<Invoice[]>> { return request({ url: '/supply/invoices/list', method: 'get', params: query }) }
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
