package com.allinone.supply.service;

import com.allinone.supply.domain.Receipt;

public interface IReceiptService {
    java.util.List<java.util.Map<String, Object>> orderOptions();
    java.util.List<java.util.Map<String, Object>> warehouseOptions();
    java.util.List<java.util.Map<String, Object>> history(Long orderId);
    Receipt prepare(Long orderId);
    int confirm(Receipt receipt);
}
