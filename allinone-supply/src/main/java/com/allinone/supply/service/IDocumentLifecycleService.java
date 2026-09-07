package com.allinone.supply.service;

public interface IDocumentLifecycleService {
    int withdraw(Long documentId, String reason);
    int voidDocument(Long documentId, String reason);
    int restoreInvoice(Long documentId, String reason);
    int deleteDraft(Long documentId);
}
