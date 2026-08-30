package com.allinone.collect.service.impl;

import com.allinone.collect.constant.CollectErrorCode;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.common.exception.ServiceException;
import com.allinone.collect.domain.CollectTemplate;
import com.allinone.collect.mapper.CollectDataCellMapper;
import com.allinone.collect.mapper.CollectDataMapper;
import com.allinone.collect.mapper.CollectTemplateMapper;
import com.allinone.collect.service.ICollectTemplateService;
import com.allinone.common.core.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CollectTemplateServiceImpl implements ICollectTemplateService {

    @Autowired
    private CollectTemplateMapper collectTemplateMapper;

    @Autowired
    private CollectDataMapper collectDataMapper;

    @Autowired
    private CollectDataCellMapper collectDataCellMapper;

    @Autowired
    private RedisCache redisCache;

    /** 模板缓存键前缀；TTL 30 分钟，模板修改/发布/删除时主动清除（总体设计 §8 缓存策略） */
    private static final String CACHE_TEMPLATE_KEY = "collect:template:";
    private static final int CACHE_TEMPLATE_TTL_MINUTES = 30;

    @Override
    public List<CollectTemplate> selectCollectTemplateList(CollectTemplate template) {
        return collectTemplateMapper.selectCollectTemplateList(template);
    }

    @Override
    public CollectTemplate selectCollectTemplateById(Long templateId) {
        String cacheKey = CACHE_TEMPLATE_KEY + templateId;
        CollectTemplate cached = redisCache.getCacheObject(cacheKey);
        if (cached != null) {
            return cached;
        }
        CollectTemplate template = collectTemplateMapper.selectCollectTemplateById(templateId);
        if (template != null) {
            redisCache.setCacheObject(cacheKey, template, CACHE_TEMPLATE_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return template;
    }

    private void evictTemplateCache(Long templateId) {
        redisCache.deleteObject(CACHE_TEMPLATE_KEY + templateId);
    }

    @Override
    public int insertCollectTemplate(CollectTemplate template) {
        checkCodeConflict(template.getTemplateCode(), null);
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
        checkCodeConflict(template.getTemplateCode(), template.getTemplateId());
        template.setStatus(null);
        template.setUpdateTime(DateUtils.getNowDate());
        template.setUpdateBy(currentUsername());
        int rows = collectTemplateMapper.updateCollectTemplate(template);
        if (rows == 0) {
            throw new ServiceException("模板已被其他用户更新，请刷新后重试", CollectErrorCode.DATA_VERSION_CONFLICT);
        }
        evictTemplateCache(template.getTemplateId());
        return rows;
    }

    /**
     * 模板编码前置查重:template_code 上的唯一索引覆盖全部行(含软删),
     * 不拦截会让重复插入以"系统未知错误"形式抛出原始 DuplicateKeyException
     */
    private void checkCodeConflict(String templateCode, Long excludeId) {
        if (templateCode == null || templateCode.isEmpty()) {
            return;
        }
        if (collectTemplateMapper.countTemplateCodeConflict(templateCode, excludeId) > 0) {
            throw new ServiceException("模板编码已存在(已删除模板的编码同样占用),请更换编码");
        }
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
            throw new ServiceException("模板不存在或已删除", CollectErrorCode.TEMPLATE_NOT_FOUND);
        }
        evictTemplateCache(templateId);
        return rows;
    }

    /**
     * 模板复制（功能模块设计 §1.1）：克隆模板定义与 Luckysheet JSON 为未发布新模板。
     * 名称追加“-副本”，编码由源编码加随机后缀生成并查重；版本从 1 重新计数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectTemplate copyTemplate(Long templateId) {
        CollectTemplate src = collectTemplateMapper.selectCollectTemplateById(templateId);
        if (src == null) {
            throw new ServiceException("模板不存在或已删除", CollectErrorCode.TEMPLATE_NOT_FOUND);
        }
        CollectTemplate copy = new CollectTemplate();
        copy.setTemplateName((src.getTemplateName() == null ? "未命名模板" : src.getTemplateName()) + "-副本");
        copy.setTemplateCode(generateCopyCode(src.getTemplateCode()));
        copy.setCategoryId(src.getCategoryId());
        copy.setTemplateType(src.getTemplateType());
        copy.setTemplateJson(src.getTemplateJson());
        copy.setRemark(src.getRemark());
        // 统一走新增：生成雪花ID/审计字段/编码查重/初始版本/未发布状态
        insertCollectTemplate(copy);
        return copy;
    }

    /** 生成复制的模板编码：源编码截断 + _copy_ + 6 位随机后缀，冲突时重试 */
    private String generateCopyCode(String sourceCode) {
        String base = (sourceCode == null ? "template" : sourceCode);
        if (base.length() > 48) {
            base = base.substring(0, 48);
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = base + "_copy_" + IdUtils.simpleUUID().substring(0, 6);
            if (collectTemplateMapper.countTemplateCodeConflict(candidate, null) == 0) {
                return candidate;
            }
        }
        throw new ServiceException("模板编码生成失败，请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCollectTemplateByIds(Long[] templateIds) {
        // 删除保护：已被非草稿填报数据或字段映射引用的模板不允许删除，避免产生孤儿数据
        if (collectTemplateMapper.countSubmittedDataByTemplateIds(templateIds) > 0) {
            throw new ServiceException("所选模板下存在已提交的填报数据，无法删除");
        }
        if (collectTemplateMapper.countFieldMappingByTemplateIds(templateIds) > 0) {
            throw new ServiceException("所选模板下存在字段映射配置，无法删除");
        }
        // 草稿级联清理：模板删除后草稿会悬挂（模板名显示为空且无法继续编辑），
        // 随模板一并逻辑删除，同时物理清理其单元格快照
        List<Long> draftIds = collectDataMapper.selectDraftDataIdsByTemplateIds(templateIds);
        if (!draftIds.isEmpty()) {
            collectDataCellMapper.deleteCollectDataCellByDataIds(draftIds);
            collectDataMapper.deleteDraftDataByTemplateIds(templateIds);
        }
        int rows = collectTemplateMapper.deleteCollectTemplateByIds(templateIds);
        for (Long templateId : templateIds) {
            evictTemplateCache(templateId);
        }
        return rows;
    }

    protected String currentUsername() {
        return SecurityUtils.getUsername();
    }
}
