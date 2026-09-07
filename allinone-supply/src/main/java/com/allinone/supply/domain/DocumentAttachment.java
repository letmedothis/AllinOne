package com.allinone.supply.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;

public class DocumentAttachment {
    private Long id;
    private Long documentId;
    private Long fileId;
    private Long versionId;
    private String attachmentType;
    private String originalName;
    private String storagePath;
    private String mediaType;
    private Long sizeBytes;
    private String sha256;
    private Long createdBy;
    private Date createdAt;

    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getDocumentId() { return documentId; } public void setDocumentId(Long v) { documentId = v; }
    public Long getFileId() { return fileId; } public void setFileId(Long v) { fileId = v; }
    public Long getVersionId() { return versionId; } public void setVersionId(Long v) { versionId = v; }
    public String getAttachmentType() { return attachmentType; } public void setAttachmentType(String v) { attachmentType = v; }
    public String getOriginalName() { return originalName; } public void setOriginalName(String v) { originalName = v; }
    @JsonIgnore public String getStoragePath() { return storagePath; } public void setStoragePath(String v) { storagePath = v; }
    public String getMediaType() { return mediaType; } public void setMediaType(String v) { mediaType = v; }
    public Long getSizeBytes() { return sizeBytes; } public void setSizeBytes(Long v) { sizeBytes = v; }
    public String getSha256() { return sha256; } public void setSha256(String v) { sha256 = v; }
    public Long getCreatedBy() { return createdBy; } public void setCreatedBy(Long v) { createdBy = v; }
    public Date getCreatedAt() { return createdAt; } public void setCreatedAt(Date v) { createdAt = v; }
}
