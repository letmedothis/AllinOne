package com.allinone.collect.service.impl;

import com.allinone.common.utils.DateUtils;
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
        template.setCreateTime(DateUtils.getNowDate());
        template.setVersion(1);
        return collectTemplateMapper.insertCollectTemplate(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCollectTemplate(CollectTemplate template) {
        template.setUpdateTime(DateUtils.getNowDate());
        // 注意：此处存在 read-then-write 竞态条件，高并发场景建议在 Mapper XML 中使用乐观锁
        // UPDATE ... SET version = version + 1 WHERE template_id = #{templateId} AND version = #{version}
        CollectTemplate existing = collectTemplateMapper.selectCollectTemplateById(template.getTemplateId());
        if (existing != null) {
            template.setVersion(existing.getVersion() + 1);
        }
        int rows = collectTemplateMapper.updateCollectTemplate(template);
        if (rows == 0 && existing != null) {
            throw new RuntimeException("模板已被其他用户更新，请刷新后重试");
        }
        return rows;
    }

    @Override
    public int updateStatus(Long templateId, String status) {
        CollectTemplate template = new CollectTemplate();
        template.setTemplateId(templateId);
        template.setStatus(status);
        template.setUpdateTime(DateUtils.getNowDate());
        return collectTemplateMapper.updateCollectTemplateStatus(template);
    }

    @Override
    public int deleteCollectTemplateByIds(Long[] templateIds) {
        return collectTemplateMapper.deleteCollectTemplateByIds(templateIds);
    }
}

