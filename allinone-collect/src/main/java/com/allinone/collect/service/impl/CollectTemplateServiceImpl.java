package com.allinone.collect.service.impl;

import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.common.exception.ServiceException;
import com.allinone.collect.domain.CollectTemplate;
import com.allinone.collect.mapper.CollectTemplateMapper;
import com.allinone.collect.service.ICollectTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CollectTemplateServiceImpl implements ICollectTemplateService {

    @Autowired
    private CollectTemplateMapper collectTemplateMapper;

    @Override
    public List<CollectTemplate> selectCollectTemplateList(CollectTemplate template) {
        return collectTemplateMapper.selectCollectTemplateList(template);
    }

    @Override
    public CollectTemplate selectCollectTemplateById(Long templateId) {
        return collectTemplateMapper.selectCollectTemplateById(templateId);
    }

    @Override
    public int insertCollectTemplate(CollectTemplate template) {
        template.setTemplateId(IdUtils.nextLongId());
        template.setCreateTime(DateUtils.getNowDate());
        template.setCreateBy(currentUsername());
        template.setVersion(1);
        template.setStatus("0");
        return collectTemplateMapper.insertCollectTemplate(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCollectTemplate(CollectTemplate template) {
        if (template.getTemplateId() == null || template.getVersion() == null) {
            throw new ServiceException("缺少模板ID或版本号");
        }
        template.setStatus(null);
        template.setUpdateTime(DateUtils.getNowDate());
        template.setUpdateBy(currentUsername());
        int rows = collectTemplateMapper.updateCollectTemplate(template);
        if (rows == 0) {
            throw new ServiceException("模板已被其他用户更新，请刷新后重试");
        }
        return rows;
    }

    @Override
    public int updateStatus(Long templateId, String status) {
        if (!"1".equals(status) && !"2".equals(status)) {
            throw new ServiceException("模板状态只能是发布或下架");
        }
        CollectTemplate template = new CollectTemplate();
        template.setTemplateId(templateId);
        template.setStatus(status);
        template.setUpdateTime(DateUtils.getNowDate());
        template.setUpdateBy(currentUsername());
        int rows = collectTemplateMapper.updateCollectTemplateStatus(template);
        if (rows == 0) {
            throw new ServiceException("模板不存在或已删除");
        }
        return rows;
    }

    @Override
    public int deleteCollectTemplateByIds(Long[] templateIds) {
        return collectTemplateMapper.deleteCollectTemplateByIds(templateIds);
    }

    protected String currentUsername() {
        return SecurityUtils.getUsername();
    }
}

