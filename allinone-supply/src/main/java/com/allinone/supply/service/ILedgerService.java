package com.allinone.supply.service;
import com.allinone.supply.domain.LedgerRow; import java.util.List;
public interface ILedgerService { List<LedgerRow> combined(String number,String supplierName,String approvalStatus); }
