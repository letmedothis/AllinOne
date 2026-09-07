package com.allinone.supply.service.impl;

import com.allinone.common.config.RuoYiConfig;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.file.FileUploadUtils;
import com.allinone.common.utils.file.FileUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.supply.domain.DocumentAttachment;
import com.allinone.supply.mapper.DocumentAttachmentMapper;
import com.allinone.supply.service.IDocumentAttachmentService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class DocumentAttachmentServiceImpl implements IDocumentAttachmentService {
    private static final String[] ALLOWED = {"pdf", "png", "jpg", "jpeg", "xml", "ofd"};
    @Autowired private DocumentAttachmentMapper mapper;

    @Override @Transactional
    public DocumentAttachment upload(Long documentId, String attachmentType, MultipartFile file) {
        if (documentId == null || file == null || file.isEmpty()) throw new ServiceException("单据和附件不能为空");
        Long creator = mapper.selectDocumentCreator(documentId);
        if (creator == null) throw new ServiceException("单据不存在");
        if (!SecurityUtils.isAdmin() && !SecurityUtils.getUserId().equals(creator)) throw new ServiceException("无权上传该单据附件");
        String status = mapper.selectDocumentStatus(documentId);
        if (!("DRAFT".equals(status) || "RETURNED".equals(status))) throw new ServiceException("单据当前状态不允许上传附件");
        if (file.getSize() > 20 * 1024 * 1024L) throw new ServiceException("单个附件不能超过20MB");
        if (mapper.countByDocument(documentId) >= 10) throw new ServiceException("单据最多保留10个附件");
        String type = mapper.selectDocumentType(documentId); String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        boolean invoice = "INVOICE".equals(type); boolean invoiceFormat = name.endsWith(".xml") || name.endsWith(".ofd") || name.endsWith(".pdf");
        boolean supportingFormat = name.endsWith(".pdf") || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
        if ((invoice && !invoiceFormat) || (!invoice && !supportingFormat)) throw new ServiceException("当前单据不支持该附件格式");
        try {
            Date now = DateUtils.getNowDate();
            byte[] bytes = file.getBytes();
            verifyContent(name, bytes);
            String path = FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file, ALLOWED, true);
            DocumentAttachment result = new DocumentAttachment();
            result.setId(IdUtils.nextLongId()); result.setFileId(IdUtils.nextLongId()); result.setDocumentId(documentId);
            result.setAttachmentType(StringUtils.isEmpty(attachmentType) ? "SUPPORTING" : attachmentType);
            result.setOriginalName(file.getOriginalFilename()); result.setStoragePath(path); result.setMediaType(file.getContentType());
            result.setSizeBytes(file.getSize()); result.setSha256(sha256(bytes)); result.setCreatedBy(SecurityUtils.getUserId()); result.setCreatedAt(now);
            mapper.insertFile(result); mapper.insertBinding(result); return result;
        } catch (Exception e) { throw new ServiceException("附件上传失败：" + e.getMessage()); }
    }

    @Override public List<DocumentAttachment> list(Long documentId) {
        Long creator = mapper.selectDocumentCreator(documentId);
        if (creator == null) throw new ServiceException("单据不存在");
        if (!SecurityUtils.isAdmin() && !hasGlobalAccess() && mapper.countDocumentViewer(documentId, SecurityUtils.getUserId()) == 0) throw new ServiceException("无权查看该单据附件");
        return mapper.selectByDocument(documentId);
    }

    @Override @Transactional public int remove(Long documentId, Long fileId) {
        Long creator = mapper.selectDocumentCreator(documentId);
        if (creator == null) throw new ServiceException("单据不存在");
        if (!SecurityUtils.isAdmin() && !SecurityUtils.getUserId().equals(creator)) throw new ServiceException("无权移除该单据附件");
        int rows = mapper.deleteBinding(documentId, fileId);
        if (rows == 0) throw new ServiceException("附件不存在、已进入历史版本或单据当前不可编辑");
        return rows;
    }

    @Override public void download(Long documentId, Long fileId, HttpServletResponse response) {
        Long creator = mapper.selectDocumentCreator(documentId);
        if (creator == null) throw new ServiceException("单据不存在");
        if (!SecurityUtils.isAdmin() && !hasGlobalAccess() && mapper.countDocumentViewer(documentId, SecurityUtils.getUserId()) == 0) throw new ServiceException("无权查看该单据附件");
        DocumentAttachment file = mapper.selectByDocumentAndFile(documentId, fileId);
        if (file == null || !FileUtils.checkAllowDownload(file.getStoragePath())) throw new ServiceException("附件不存在或路径非法");
        try {
            String localPath = RuoYiConfig.getProfile() + FileUtils.stripPrefix(file.getStoragePath());
            response.setContentType(StringUtils.isEmpty(file.getMediaType()) ? "application/octet-stream" : file.getMediaType());
            FileUtils.setAttachmentResponseHeader(response, file.getOriginalName());
            FileUtils.writeBytes(localPath, response.getOutputStream());
        } catch (Exception e) { throw new ServiceException("附件下载失败"); }
    }

    private String sha256(byte[] data) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(data); StringBuilder out = new StringBuilder();
        for (byte item : hash) out.append(String.format("%02x", item)); return out.toString();
    }

    private void verifyContent(String name, byte[] bytes) {
        if (name.endsWith(".pdf") && !startsWith(bytes, "%PDF-")) throw new ServiceException("PDF 文件内容与扩展名不一致");
        if (name.endsWith(".png") && !startsWith(bytes, new byte[] {(byte) 0x89, 'P', 'N', 'G'})) throw new ServiceException("PNG 文件内容与扩展名不一致");
        if ((name.endsWith(".jpg") || name.endsWith(".jpeg")) && !(bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff)) throw new ServiceException("JPEG 文件内容与扩展名不一致");
        if (name.endsWith(".ofd") && !startsWith(bytes, new byte[] {'P', 'K'})) throw new ServiceException("OFD 文件内容与扩展名不一致");
        if (name.endsWith(".xml")) { String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).stripLeading(); if (!text.startsWith("<")) throw new ServiceException("XML 文件内容与扩展名不一致"); }
    }

    private boolean startsWith(byte[] value, String prefix) { return startsWith(value, prefix.getBytes(java.nio.charset.StandardCharsets.US_ASCII)); }
    private boolean startsWith(byte[] value, byte[] prefix) { if (value.length < prefix.length) return false; for (int i = 0; i < prefix.length; i++) if (value[i] != prefix[i]) return false; return true; }
    private boolean hasGlobalAccess() { return SecurityUtils.getLoginUser() != null && SecurityUtils.getLoginUser().getUser().getRoles() != null && SecurityUtils.getLoginUser().getUser().getRoles().stream().anyMatch(role -> java.util.Arrays.asList("supervisor", "purchasing_supervisor", "purchase_supervisor", "finance").contains(role.getRoleKey())); }
}
