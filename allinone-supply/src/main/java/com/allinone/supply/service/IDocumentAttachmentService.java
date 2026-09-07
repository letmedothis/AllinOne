package com.allinone.supply.service;

import com.allinone.supply.domain.DocumentAttachment;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IDocumentAttachmentService {
    DocumentAttachment upload(Long documentId, String attachmentType, MultipartFile file);
    List<DocumentAttachment> list(Long documentId);
    int remove(Long documentId, Long fileId);
    void download(Long documentId, Long fileId, HttpServletResponse response);
}
