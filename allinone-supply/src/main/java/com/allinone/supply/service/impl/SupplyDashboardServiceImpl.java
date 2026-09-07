package com.allinone.supply.service.impl;
import com.allinone.supply.domain.SupplyDashboard;
import com.allinone.supply.mapper.SupplyDashboardMapper;
import com.allinone.supply.service.ISupplyDashboardService;
import com.allinone.supply.support.SupplyDataScopeResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SupplyDashboardServiceImpl implements ISupplyDashboardService {
    @Autowired private SupplyDashboardMapper mapper;
    @Autowired private SupplyDataScopeResolver scopeResolver;

    @Override
    public SupplyDashboard summary() {
        SupplyDataScopeResolver.Scope scope = scopeResolver.current();
        return mapper.selectSummary(scope.mode(), scope.userId(), scope.deptId());
    }
}
