package com.allinone.supply.controller;

import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.supply.service.IDocumentAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/supply/documents")
public class DocumentAttachmentController extends BaseController {
    @Autowired private IDocumentAttachmentService service;

    @PreAuthorize("@ss.hasPermi('supply:supplier:query') or @ss.hasPermi('supply:order:query') or @ss.hasPermi('supply:invoice:query')")
    @GetMapping("/{documentId}/attachments")
    public AjaxResult list(@PathVariable Long documentId) { return success(service.list(documentId)); }

    @PreAuthorize("@ss.hasAnyPermi('supply:supplier:add,supply:supplier:edit,supply:order:edit,supply:invoice:edit')")
    @PostMapping(value = "/{documentId}/attachments", consumes = "multipart/form-data")
    public AjaxResult upload(@PathVariable Long documentId, @RequestParam(value = "attachmentType", required = false) String attachmentType,
                             @RequestParam("file") MultipartFile file) { return success(service.upload(documentId, attachmentType, file)); }

    @PreAuthorize("@ss.hasAnyPermi('supply:supplier:edit,supply:order:edit,supply:invoice:edit')")
    @DeleteMapping("/{documentId}/attachments/{fileId}")
    public AjaxResult remove(@PathVariable Long documentId, @PathVariable Long fileId) { return toAjax(service.remove(documentId, fileId)); }

    @PreAuthorize("@ss.hasAnyPermi('supply:supplier:query,supply:order:query,supply:invoice:query')")
    @GetMapping("/{documentId}/attachments/{fileId}")
    public void download(@PathVariable Long documentId, @PathVariable Long fileId, HttpServletResponse response) { service.download(documentId, fileId, response); }
}
