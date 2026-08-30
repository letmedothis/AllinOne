package com.allinone.collect.domain;

import com.allinone.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * 填报数据异步导出任务。
 * 生命周期：pending（排队）→ running（导出中）→ success / failed。
 * 生成的文件落在 RuoYiConfig.getDownloadPath()，仅任务创建者（或管理员）可下载。
 */
public class CollectExportTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long taskId;

    private String taskName;

    /** 导出筛选条件（CollectData 序列化 JSON，供后台任务复现查询） */
    private String queryJson;

    /** 状态: pending | running | success | failed */
    private String status;

    /** 生成的导出文件名 */
    private String fileName;

    /** 失败原因 */
    private String errorMsg;

    private Date finishTime;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getQueryJson() { return queryJson; }
    public void setQueryJson(String queryJson) { this.queryJson = queryJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public Date getFinishTime() { return finishTime; }
    public void setFinishTime(Date finishTime) { this.finishTime = finishTime; }
}
