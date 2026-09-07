package com.allinone.supply.mapper;

import com.allinone.supply.domain.DocumentAttachment;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DocumentAttachmentMapper {
    String selectDocumentStatus(@Param("documentId") Long documentId);
    String selectDocumentType(@Param("documentId") Long documentId);
    Long selectDocumentCreator(@Param("documentId") Long documentId);
    int countDocumentViewer(@Param("documentId") Long documentId, @Param("userId") Long userId);
    int countByDocument(@Param("documentId") Long documentId);
    int insertFile(DocumentAttachment file);
    int insertBinding(DocumentAttachment file);
    List<DocumentAttachment> selectByDocument(@Param("documentId") Long documentId);
    DocumentAttachment selectByDocumentAndFile(@Param("documentId") Long documentId, @Param("fileId") Long fileId);
    int deleteBinding(@Param("documentId") Long documentId, @Param("fileId") Long fileId);
}
