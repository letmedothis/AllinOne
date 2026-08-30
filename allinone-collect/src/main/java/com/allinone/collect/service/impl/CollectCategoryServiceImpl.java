package com.allinone.collect.service.impl;

import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.common.exception.ServiceException;
import com.allinone.collect.domain.CollectCategory;
import com.allinone.collect.mapper.CollectCategoryMapper;
import com.allinone.collect.service.ICollectCategoryService;
import com.allinone.common.core.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CollectCategoryServiceImpl implements ICollectCategoryService {

    @Autowired
    private CollectCategoryMapper collectCategoryMapper;

    @Autowired
    private RedisCache redisCache;

    /** 全量分类列表缓存键；TTL 1 小时，分类增删改时主动清除（总体设计 §8 缓存策略） */
    private static final String CACHE_CATEGORY_LIST_KEY = "collect:category:list";
    private static final int CACHE_CATEGORY_TTL_HOURS = 1;

    @Override
    public List<CollectCategory> selectCollectCategoryList(CollectCategory category) {
        // 仅缓存无筛选条件的全量查询（树形组装依赖全量数据）
        boolean unfiltered = category.getCategoryName() == null && category.getStatus() == null;
        if (unfiltered) {
            List<CollectCategory> cached = redisCache.getCacheObject(CACHE_CATEGORY_LIST_KEY);
            if (cached != null) {
                return cached;
            }
        }
        List<CollectCategory> list = collectCategoryMapper.selectCollectCategoryList(category);
        if (unfiltered) {
            redisCache.setCacheObject(CACHE_CATEGORY_LIST_KEY, list, CACHE_CATEGORY_TTL_HOURS, TimeUnit.HOURS);
        }
        return list;
    }

    @Override
    public CollectCategory selectCollectCategoryById(Long categoryId) {
        return collectCategoryMapper.selectCollectCategoryById(categoryId);
    }

    @Override
    public int insertCollectCategory(CollectCategory category) {
        category.setCategoryId(IdUtils.nextLongId());
        category.setCreateTime(DateUtils.getNowDate());
        category.setCreateBy(SecurityUtils.getUsername());
        int rows = collectCategoryMapper.insertCollectCategory(category);
        redisCache.deleteObject(CACHE_CATEGORY_LIST_KEY);
        return rows;
    }

    @Override
    public int updateCollectCategory(CollectCategory category) {
        category.setUpdateTime(DateUtils.getNowDate());
        category.setUpdateBy(SecurityUtils.getUsername());
        int rows = collectCategoryMapper.updateCollectCategory(category);
        redisCache.deleteObject(CACHE_CATEGORY_LIST_KEY);
        return rows;
    }

    @Override
    public int deleteCollectCategoryByIds(Long[] categoryIds) {
        // 删除保护：存在子分类时先删子分类，避免树断裂
        if (collectCategoryMapper.countCategoryChildByCategoryIds(categoryIds) > 0) {
            throw new ServiceException("所选分类下存在子分类，请先删除子分类");
        }
        // 删除保护：分类下仍存在填报模板时不允许删除，避免模板失去分类归属
        if (collectCategoryMapper.countTemplateByCategoryIds(categoryIds) > 0) {
            throw new ServiceException("所选分类下存在填报模板，无法删除");
        }
        int rows = collectCategoryMapper.deleteCollectCategoryByIds(categoryIds);
        redisCache.deleteObject(CACHE_CATEGORY_LIST_KEY);
        return rows;
    }
}

