package com.allinone.supply.controller;

import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.supply.service.IDocumentLifecycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/supply/documents")
public class DocumentLifecycleController extends BaseController {
    @Autowired private IDocumentLifecycleService service;
    public static class Reason { private String reason; public String getReason(){return reason;} public void setReason(String v){reason=v;} }
    @PreAuthorize("@ss.hasAnyPermi('supply:supplier:edit,supply:order:edit,supply:invoice:edit')") @PostMapping("/{id}/withdraw") public AjaxResult withdraw(@PathVariable Long id,@RequestBody Reason body){return toAjax(service.withdraw(id,body==null?null:body.getReason()));}
    @PreAuthorize("@ss.hasAnyPermi('supply:supplier:edit,supply:order:edit,supply:invoice:edit')") @PostMapping("/{id}/void") public AjaxResult voidDocument(@PathVariable Long id,@RequestBody Reason body){return toAjax(service.voidDocument(id,body==null?null:body.getReason()));}
    @PreAuthorize("@ss.hasPermi('supply:invoice:edit')") @PostMapping("/{id}/restore") public AjaxResult restore(@PathVariable Long id,@RequestBody Reason body){return toAjax(service.restoreInvoice(id,body==null?null:body.getReason()));}
    @PreAuthorize("@ss.hasAnyPermi('supply:supplier:edit,supply:order:edit,supply:invoice:edit')") @DeleteMapping("/{id}") public AjaxResult delete(@PathVariable Long id){return toAjax(service.deleteDraft(id));}
}
