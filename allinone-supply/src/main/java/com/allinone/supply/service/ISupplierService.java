package com.allinone.supply.service;

import com.allinone.supply.domain.Supplier;
import java.util.List;

public interface ISupplierService {
    List<Supplier> selectSupplierList(Supplier supplier);
    Supplier selectSupplierById(Long documentId);
    int insertSupplier(Supplier supplier);
    int updateSupplier(Supplier supplier);
    int submitSupplier(Long documentId);
}
