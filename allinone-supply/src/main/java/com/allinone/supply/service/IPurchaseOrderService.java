package com.allinone.supply.service;

import com.allinone.supply.domain.PurchaseOrder;
import com.allinone.supply.domain.SupplierOption;
import java.util.List;

public interface IPurchaseOrderService {
    List<PurchaseOrder> selectPurchaseOrderList(PurchaseOrder order);
    List<SupplierOption> selectApprovedSupplierOptions();
    PurchaseOrder selectPurchaseOrderById(Long documentId);
    int insertPurchaseOrder(PurchaseOrder order);
    int updatePurchaseOrder(PurchaseOrder order);
    int submitPurchaseOrder(Long documentId);
}
