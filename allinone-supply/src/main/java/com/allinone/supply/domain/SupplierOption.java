package com.allinone.supply.domain;

/** 采购订单选择供应商时使用的最小字段集合。 */
public class SupplierOption {
    private String documentId;
    private String name;
    private String taxId;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }
}
