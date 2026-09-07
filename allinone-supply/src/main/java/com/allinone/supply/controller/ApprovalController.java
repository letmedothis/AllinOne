package com.allinone.supply.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.supply.domain.ApprovalDecision;
import com.allinone.supply.domain.ApprovalTask;
import com.allinone.supply.service.IApprovalService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supply/approvals")
public class ApprovalController extends BaseController {
    @Autowired private IApprovalService service;
    @PreAuthorize("@ss.hasPermi('supply:approval:list')") @GetMapping("/pending")
    public TableDataInfo pending() { startPage(); List<ApprovalTask> list = service.selectPendingTasks(); return getDataTable(list); }
    @PreAuthorize("@ss.hasPermi('supply:approval:approve')") @Log(title="供应链审批", businessType=BusinessType.UPDATE) @PostMapping("/approve")
    public AjaxResult approve(@RequestBody ApprovalDecision decision) { return toAjax(service.approve(decision)); }
    @PreAuthorize("@ss.hasPermi('supply:approval:reject')") @Log(title="供应链审批", businessType=BusinessType.UPDATE) @PostMapping("/reject")
    public AjaxResult reject(@RequestBody ApprovalDecision decision) { return toAjax(service.reject(decision)); }
}
