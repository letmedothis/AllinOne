package com.allinone.supply.controller;

import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.annotation.RepeatSubmit;
import com.allinone.supply.domain.OpeningPayable;
import com.allinone.supply.mapper.PaymentMapper;
import com.allinone.supply.service.impl.OpeningPayableService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/supply/opening-payables")
public class OpeningPayableController extends BaseController {
    @Autowired private OpeningPayableService service;
    @Autowired private PaymentMapper payments;
    @PreAuthorize("@ss.hasPermi('supply:opening:manage')") @GetMapping("/list")
    public AjaxResult list() { return success(service.list()); }
    @PreAuthorize("@ss.hasPermi('supply:opening:manage')") @GetMapping("/supplier-options")
    public AjaxResult supplierOptions() { return success(payments.selectApprovedSupplierOptions()); }
    @PreAuthorize("@ss.hasPermi('supply:opening:manage')") @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) { return success(service.get(id)); }
    @PreAuthorize("@ss.hasPermi('supply:opening:manage')") @RepeatSubmit @PostMapping
    public AjaxResult save(@Valid @RequestBody OpeningPayable request) { return success(service.save(request)); }
    @PreAuthorize("@ss.hasPermi('supply:opening:manage')") @RepeatSubmit @PostMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable Long id,@RequestParam Integer revision) { return toAjax(service.submit(id,revision)); }
}
