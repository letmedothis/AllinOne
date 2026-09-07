package com.allinone.supply.service;

import com.allinone.supply.domain.PurchaseOrder;
import java.util.List;

public interface IPurchaseOrderService {
    List<PurchaseOrder> selectPurchaseOrderList(PurchaseOrder order);
    PurchaseOrder selectPurchaseOrderById(Long documentId);
    int insertPurchaseOrder(PurchaseOrder order);
    int updatePurchaseOrder(PurchaseOrder order);
    int submitPurchaseOrder(Long documentId);
}
