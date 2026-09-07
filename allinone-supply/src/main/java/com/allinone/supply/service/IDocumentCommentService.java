package com.allinone.supply.service;

import com.allinone.supply.domain.DocumentComment;
import java.util.List;

public interface IDocumentCommentService { List<DocumentComment> list(Long documentId); int add(Long documentId, String content); }
