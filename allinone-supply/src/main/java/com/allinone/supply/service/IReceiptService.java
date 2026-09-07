package com.allinone.supply.service;

import com.allinone.supply.domain.Receipt;

public interface IReceiptService {
    Receipt prepare(Long orderId);
    int confirm(Receipt receipt);
}
