package com.allinone.supply.mapper;

import com.allinone.supply.domain.DocumentComment;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DocumentCommentMapper {
    int countDocument(@Param("documentId") Long documentId);
    int countViewer(@Param("documentId") Long documentId, @Param("userId") Long userId);
    Integer selectCurrentVersion(@Param("documentId") Long documentId);
    List<DocumentComment> selectByDocument(@Param("documentId") Long documentId);
    int insert(DocumentComment comment);
}
