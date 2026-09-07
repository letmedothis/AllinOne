package com.allinone.supply.controller;

import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.supply.service.IDocumentCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/supply/documents/{documentId}/comments") public class DocumentCommentController extends BaseController {
    @Autowired private IDocumentCommentService service;
    public static class Body { private String content; public String getContent(){return content;} public void setContent(String v){content=v;} }
    @PreAuthorize("@ss.hasAnyPermi('supply:supplier:query,supply:order:query,supply:invoice:query,supply:receipt:query')") @GetMapping public AjaxResult list(@PathVariable Long documentId){return success(service.list(documentId));}
    @PreAuthorize("@ss.hasAnyPermi('supply:supplier:edit,supply:order:edit,supply:invoice:edit,supply:receipt:add')") @PostMapping public AjaxResult add(@PathVariable Long documentId,@RequestBody Body body){return toAjax(service.add(documentId,body==null?null:body.getContent()));}
}
