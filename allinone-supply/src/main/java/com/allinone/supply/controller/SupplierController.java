package com.allinone.supply.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.annotation.RepeatSubmit;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.supply.domain.Supplier;
import com.allinone.supply.service.ISupplierService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/supply/suppliers")
public class SupplierController extends BaseController {
    @Autowired
    private ISupplierService supplierService;

    @PreAuthorize("@ss.hasPermi('supply:supplier:list')")
    @GetMapping("/list")
    public TableDataInfo list(Supplier supplier) {
        startPage();
        List<Supplier> list = supplierService.selectSupplierList(supplier);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('supply:supplier:query')")
    @GetMapping("/{documentId}")
    public AjaxResult getInfo(@PathVariable Long documentId) {
        return success(supplierService.selectSupplierById(documentId));
    }

    @PreAuthorize("@ss.hasPermi('supply:supplier:add')")
    @Log(title = "供应商", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Supplier supplier) {
        supplierService.insertSupplier(supplier);
        return success(supplier);
    }

    @PreAuthorize("@ss.hasPermi('supply:supplier:edit')")
    @Log(title = "供应商", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Supplier supplier) {
        return toAjax(supplierService.updateSupplier(supplier));
    }

    @PreAuthorize("@ss.hasPermi('supply:supplier:edit')")
    @Log(title = "供应商", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{documentId}/submit")
    public AjaxResult submit(@PathVariable Long documentId) {
        return toAjax(supplierService.submitSupplier(documentId));
    }
}
