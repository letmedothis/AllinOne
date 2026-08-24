package com.allinone.collect.domain;

import com.allinone.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 填报分类表 collect_category
 */
public class CollectCategory extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private String categoryName;
    private Long parentId;
    private String ancestors;
    private Integer orderNum;
    private String status;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getAncestors() { return ancestors; }
    public void setAncestors(String ancestors) { this.ancestors = ancestors; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() { return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
        .append("categoryId", getCategoryId())
        .append("categoryName", getCategoryName())
        .append("parentId", getParentId())
        .append("orderNum", getOrderNum())
        .append("status", getStatus())
        .append("createBy", getCreateBy())
        .append("createTime", getCreateTime()).toString();
    }
}

