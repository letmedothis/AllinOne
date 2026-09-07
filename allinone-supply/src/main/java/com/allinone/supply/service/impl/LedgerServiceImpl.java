package com.allinone.supply.service.impl;
import com.allinone.supply.domain.LedgerRow;
import com.allinone.supply.mapper.LedgerMapper;
import com.allinone.supply.service.ILedgerService;
import com.allinone.supply.support.SupplyDataScopeResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LedgerServiceImpl implements ILedgerService {
    @Autowired private LedgerMapper mapper;
    @Autowired private SupplyDataScopeResolver scopeResolver;

    @Override
    public List<LedgerRow> combined(String number, String supplierName, String approvalStatus) {
        SupplyDataScopeResolver.Scope scope = scopeResolver.current();
        return mapper.selectCombined(scope.mode(), scope.userId(), scope.deptId(), number, supplierName, approvalStatus);
    }
}
