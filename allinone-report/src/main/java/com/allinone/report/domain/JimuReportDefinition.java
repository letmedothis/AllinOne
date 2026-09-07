package com.allinone.report.domain;

import java.util.Date;

/**
 * JimuReport 引擎中可供应用配置引用的报表目录项。
 *
 * <p>这是对引擎 {@code jimu_report} 表的只读映射，不归应用侧管理。</p>
 */
public class JimuReportDefinition
{
    private String id;
    private String code;
    private String name;
    private String status;
    private Date updateTime;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
