package com.allinone.supply.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.annotation.RepeatSubmit;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.enums.BusinessType;
import com.allinone.supply.domain.Receipt;
import com.allinone.supply.service.IReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supply/receipts")
public class ReceiptController extends BaseController {
    @Autowired private IReceiptService service;
    @PreAuthorize("@ss.hasPermi('supply:receipt:add')") @GetMapping("/order-options")
    public AjaxResult orders() { return success(service.orderOptions()); }
    @PreAuthorize("@ss.hasPermi('supply:receipt:add')") @GetMapping("/warehouse-options")
    public AjaxResult warehouses() { return success(service.warehouseOptions()); }
    @PreAuthorize("@ss.hasPermi('supply:receipt:add')") @GetMapping("/history")
    public com.allinone.common.core.page.TableDataInfo history(@RequestParam(required=false) Long orderId) { startPage(); return getDataTable(service.history(orderId)); }
    @PreAuthorize("@ss.hasPermi('supply:receipt:add')") @GetMapping("/prepare/{orderId}")
    public AjaxResult prepare(@PathVariable Long orderId) { return success(service.prepare(orderId)); }
    @PreAuthorize("@ss.hasPermi('supply:receipt:add')") @Log(title="入库确认", businessType=BusinessType.INSERT) @RepeatSubmit @PostMapping("/confirm")
    public AjaxResult confirm(@RequestBody Receipt receipt) { return toAjax(service.confirm(receipt)); }
}
