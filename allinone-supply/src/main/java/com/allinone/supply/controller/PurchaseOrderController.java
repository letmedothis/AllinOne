package com.allinone.supply.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.annotation.RepeatSubmit;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.supply.domain.PurchaseOrder;
import com.allinone.supply.domain.SupplierOption;
import com.allinone.supply.service.IPurchaseOrderService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supply/orders")
public class PurchaseOrderController extends BaseController {
    @Autowired private IPurchaseOrderService service;
    @PreAuthorize("@ss.hasPermi('supply:order:list')") @GetMapping("/list")
    public TableDataInfo list(PurchaseOrder order) { startPage(); List<PurchaseOrder> list = service.selectPurchaseOrderList(order); return getDataTable(list); }
    @PreAuthorize("@ss.hasAnyPermi('supply:order:add,supply:order:edit')") @GetMapping("/supplier-options")
    public AjaxResult supplierOptions() { return success(service.selectApprovedSupplierOptions()); }
    @PreAuthorize("@ss.hasPermi('supply:order:query')") @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) { return success(service.selectPurchaseOrderById(id)); }
    @PreAuthorize("@ss.hasPermi('supply:order:add')") @Log(title="采购订单", businessType=BusinessType.INSERT) @PostMapping
    public AjaxResult add(@RequestBody PurchaseOrder order) { return toAjax(service.insertPurchaseOrder(order)); }
    @PreAuthorize("@ss.hasPermi('supply:order:edit')") @Log(title="采购订单", businessType=BusinessType.UPDATE) @PutMapping
    public AjaxResult edit(@RequestBody PurchaseOrder order) { return toAjax(service.updatePurchaseOrder(order)); }
    @PreAuthorize("@ss.hasPermi('supply:order:edit')") @Log(title="采购订单", businessType=BusinessType.UPDATE) @RepeatSubmit @PostMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable Long id) { return toAjax(service.submitPurchaseOrder(id)); }
}
