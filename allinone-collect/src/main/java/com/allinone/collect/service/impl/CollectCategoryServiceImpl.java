package com.allinone.collect.service.impl;

import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.common.exception.ServiceException;
import com.allinone.collect.domain.CollectCategory;
import com.allinone.collect.mapper.CollectCategoryMapper;
import com.allinone.collect.service.ICollectCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CollectCategoryServiceImpl implements ICollectCategoryService {

    @Autowired
    private CollectCategoryMapper collectCategoryMapper;

    @Override
    public List<CollectCategory> selectCollectCategoryList(CollectCategory category) {
        return collectCategoryMapper.selectCollectCategoryList(category);
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
        return collectCategoryMapper.insertCollectCategory(category);
    }

    @Override
    public int updateCollectCategory(CollectCategory category) {
        category.setUpdateTime(DateUtils.getNowDate());
        category.setUpdateBy(SecurityUtils.getUsername());
        return collectCategoryMapper.updateCollectCategory(category);
    }

    @Override
    public int deleteCollectCategoryByIds(Long[] categoryIds) {
        // 删除保护：分类下仍存在填报模板时不允许删除，避免模板失去分类归属
        if (collectCategoryMapper.countTemplateByCategoryIds(categoryIds) > 0) {
            throw new ServiceException("所选分类下存在填报模板，无法删除");
        }
        return collectCategoryMapper.deleteCollectCategoryByIds(categoryIds);
    }
}

