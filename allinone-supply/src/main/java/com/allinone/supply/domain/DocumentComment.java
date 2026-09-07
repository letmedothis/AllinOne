package com.allinone.supply.domain;

import java.util.Date;

public class DocumentComment {
    private Long id; private Long documentId; private Long versionId; private Long authorId; private String authorSnapshot; private String content; private Date createdAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;} public Long getVersionId(){return versionId;} public void setVersionId(Long v){versionId=v;} public Long getAuthorId(){return authorId;} public void setAuthorId(Long v){authorId=v;} public String getAuthorSnapshot(){return authorSnapshot;} public void setAuthorSnapshot(String v){authorSnapshot=v;} public String getContent(){return content;} public void setContent(String v){content=v;} public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
}
