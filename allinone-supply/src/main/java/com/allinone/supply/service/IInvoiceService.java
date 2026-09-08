package com.allinone.supply.service;
import com.allinone.supply.domain.Invoice; import java.util.List;
public interface IInvoiceService { Invoice get(Long id); List<Invoice> list(String invoiceNumber,String approvalStatus); int createDraft(Invoice invoice); int updateDraft(Invoice invoice); int confirm(Invoice invoice); int submit(Invoice invoice); }
