package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectData;
import com.allinone.collect.mapper.CollectDataMapper;
import com.allinone.collect.mapper.CollectTemplateMapper;
import com.allinone.common.approval.ReviewCase;
import com.allinone.common.approval.ReviewHandler;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CollectCorrectionReviewHandler implements ReviewHandler {
    @Autowired private CollectDataMapper dataMapper;
    @Autowired private CollectTemplateMapper templateMapper;
    @Autowired private CollectDataServiceImpl dataService;
    private final ObjectMapper json=new ObjectMapper();
    public String type(){return "COLLECT_CORRECTION";}
    public void prepare(ReviewCase c,boolean creating){
        if(c.targetId==null)throw new ServiceException("请选择已提交填报");
        CollectData d=dataMapper.selectSubmittedForCorrection(c.targetId); if(d==null)throw new ServiceException("只能对已提交填报申请更正");
        if(creating){c.ownerId=SecurityUtils.getUserId();c.deptId=d.getDeptId();c.baseVersion=d.getVersion();c.title="填报更正："+d.getDataCode();c.beforeJson=source(d);}
        if(c.afterJson==null||c.afterJson.length()>5_000_000)throw new ServiceException("更正内容为空或过大");
        try{json.readTree(c.afterJson);}catch(Exception e){throw new ServiceException("更正工作簿 JSON 格式不正确");}
    }
    public void validateSubmission(ReviewCase c){
        CollectData d=dataMapper.selectSubmittedForCorrection(c.targetId);if(d==null||!Objects.equals(d.getVersion(),c.baseVersion))throw new ServiceException("原填报已更新，请重新生成更正申请");
        if(c.afterJson.equals(d.getFormData()))throw new ServiceException("没有检测到填报变更");
        if(templateMapper.selectTemplateVersion(d.getTemplateId(),d.getTemplateVersion())==null)throw new ServiceException("原模板版本缺失，不能更正");
    }
    public void apply(ReviewCase c){validateSubmission(c);dataService.applyCorrection(c.targetId,c.baseVersion,c.afterJson,c.id);}
    public void submitted(ReviewCase c){validateSubmission(c);}
    public void cancelled(ReviewCase c){}
    private String source(CollectData d){try{return json.writeValueAsString(Map.of("templateId",d.getTemplateId(),"templateVersion",d.getTemplateVersion(),"version",d.getVersion(),"formData",d.getFormData()));}catch(Exception e){throw new ServiceException("原始填报快照生成失败");}}
}
