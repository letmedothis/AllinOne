package com.allinone.supply.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.annotation.RepeatSubmit;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.supply.domain.ApBalanceRow;
import com.allinone.supply.domain.Payment;
import com.allinone.supply.domain.PaymentDecision;
import com.allinone.supply.service.IPaymentService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 付款 / 应付核销(M4)。 */
@RestController
@RequestMapping("/supply/payments")
public class PaymentController extends BaseController {
    @Autowired private IPaymentService service;

    @PreAuthorize("@ss.hasPermi('supply:payment:query')")
    @GetMapping("/list")
    public TableDataInfo list(Payment filter) { startPage(); List<Payment> list = service.list(filter); return getDataTable(list); }

    @PreAuthorize("@ss.hasPermi('supply:payment:add')")
    @GetMapping("/supplier-options")
    public AjaxResult supplierOptions() { return success(service.supplierOptions()); }

    @PreAuthorize("@ss.hasAnyPermi('supply:payment:query,supply:payment:approve')")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) { return success(service.get(id)); }

    @PreAuthorize("@ss.hasPermi('supply:payment:add')")
    @Log(title = "付款单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Payment payment) { return toAjax(service.create(payment)); }

    @PreAuthorize("@ss.hasPermi('supply:payment:edit')")
    @Log(title = "付款单", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public AjaxResult edit(@RequestBody Payment payment) { return toAjax(service.update(payment)); }

    @PreAuthorize("@ss.hasPermi('supply:payment:edit')")
    @Log(title = "付款单提交", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable Long id) { return toAjax(service.submit(id)); }

    @PreAuthorize("@ss.hasPermi('supply:payment:approve')")
    @Log(title = "付款财务复核", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/review")
    public AjaxResult review(@PathVariable Long id, @RequestBody PaymentDecision decision) { return toAjax(service.review(id, decision)); }

    @PreAuthorize("@ss.hasPermi('supply:payment:approve')")
    @Log(title = "付款总监终审", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/director-decision")
    public AjaxResult directorDecision(@PathVariable Long id, @RequestBody PaymentDecision decision) { return toAjax(service.directorDecision(id, decision)); }

    @PreAuthorize("@ss.hasPermi('supply:payment:edit')")
    @Log(title = "付款单作废", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/void")
    public AjaxResult voidPayment(@PathVariable Long id, @RequestParam(required = false) String reason) { return toAjax(service.voidPayment(id, reason)); }

    @PreAuthorize("@ss.hasPermi('supply:payment:query')")
    @GetMapping("/ap-balance")
    public AjaxResult apBalance(@RequestParam(required = false) Long supplierId) { return success(service.apBalance(supplierId)); }
}
