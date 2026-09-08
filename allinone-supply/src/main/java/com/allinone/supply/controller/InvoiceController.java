package com.allinone.supply.controller;

import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.supply.service.InvoiceXmlParser; import com.allinone.supply.service.IInvoiceService; import com.allinone.common.core.page.TableDataInfo; import java.util.List; import com.allinone.supply.domain.Invoice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supply/invoices")
public class InvoiceController extends BaseController {
    @org.springframework.beans.factory.annotation.Autowired private com.allinone.supply.service.IPurchaseOrderService orders;
    @PreAuthorize("@ss.hasAnyPermi('supply:invoice:add,supply:invoice:edit')")
    @GetMapping("/order-options") public AjaxResult orderOptions() {
        var filter = new com.allinone.supply.domain.PurchaseOrder(); filter.setApprovalStatus("APPROVED");
        return success(orders.selectPurchaseOrderList(filter));
    }
    @PreAuthorize("@ss.hasAnyPermi('supply:invoice:add,supply:invoice:edit')")
    @GetMapping("/orders/{id}") public AjaxResult order(@PathVariable Long id) { return success(orders.selectPurchaseOrderById(id)); }
    private final InvoiceXmlParser parser = new InvoiceXmlParser(); @org.springframework.beans.factory.annotation.Autowired private IInvoiceService invoiceService;
    @PreAuthorize("@ss.hasPermi('supply:invoice:query')") @GetMapping("/list") public TableDataInfo list(@RequestParam(required=false) String invoiceNumber,@RequestParam(required=false) String approvalStatus){startPage();List<Invoice> list=invoiceService.list(invoiceNumber,approvalStatus);return getDataTable(list);}
    @PreAuthorize("@ss.hasPermi('supply:invoice:add')")
    @PostMapping("/parse-xml")
    public AjaxResult parseXml(@RequestBody String xml) { return success(parser.parse(xml)); }
    @PreAuthorize("@ss.hasAnyPermi('supply:invoice:query,supply:invoice:edit,supply:approval:approve,supply:approval:reject')")
    @GetMapping("/{id}") public AjaxResult get(@PathVariable Long id) { return success(invoiceService.get(id)); }
}
