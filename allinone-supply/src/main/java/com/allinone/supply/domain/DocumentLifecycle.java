package com.allinone.supply.domain;

public class DocumentLifecycle {
    private Long documentId; private Long creatorId; private Integer revision; private Integer currentVersion; private String type; private String status; private String workflowEngine;
    public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;} public Long getCreatorId(){return creatorId;} public void setCreatorId(Long v){creatorId=v;} public Integer getRevision(){return revision;} public void setRevision(Integer v){revision=v;} public Integer getCurrentVersion(){return currentVersion;} public void setCurrentVersion(Integer v){currentVersion=v;} public String getType(){return type;} public void setType(String v){type=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getWorkflowEngine(){return workflowEngine;} public void setWorkflowEngine(String v){workflowEngine=v;}
}
