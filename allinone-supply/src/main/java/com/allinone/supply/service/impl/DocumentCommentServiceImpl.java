package com.allinone.supply.service.impl;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.supply.domain.DocumentComment;
import com.allinone.supply.mapper.DocumentCommentMapper;
import com.allinone.supply.service.IDocumentCommentService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class DocumentCommentServiceImpl implements IDocumentCommentService {
    @Autowired private DocumentCommentMapper mapper;
    @Override public List<DocumentComment> list(Long documentId) { requireVisible(documentId); return mapper.selectByDocument(documentId); }
    @Override @Transactional public int add(Long documentId, String content) { requireVisible(documentId); if(content==null||content.trim().isEmpty()||content.length()>2000) throw new ServiceException("评论不能为空且不能超过2000个字符"); DocumentComment c=new DocumentComment(); c.setId(IdUtils.nextLongId()); c.setDocumentId(documentId); Integer version = mapper.selectCurrentVersion(documentId); c.setVersionId(version == null ? null : version.longValue()); c.setAuthorId(SecurityUtils.getUserId()); c.setAuthorSnapshot(SecurityUtils.getUsername()); c.setContent(content.trim()); c.setCreatedAt(DateUtils.getNowDate()); return mapper.insert(c); }
    private void requireVisible(Long documentId){if(documentId==null||mapper.countDocument(documentId)==0)throw new ServiceException("单据不存在或无权查看"); if(SecurityUtils.isAdmin() || hasGlobalAccess()) return; if(mapper.countViewer(documentId,SecurityUtils.getUserId())==0)throw new ServiceException("单据不存在或无权查看");}
    private boolean hasGlobalAccess(){return SecurityUtils.getLoginUser()!=null&&SecurityUtils.getLoginUser().getUser().getRoles()!=null&&SecurityUtils.getLoginUser().getUser().getRoles().stream().anyMatch(role->java.util.Arrays.asList("supervisor","purchasing_supervisor","purchase_supervisor","finance").contains(role.getRoleKey()));}
}
