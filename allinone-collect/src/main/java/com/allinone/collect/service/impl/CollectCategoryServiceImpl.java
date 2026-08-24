package com.allinone.collect.service.impl;

import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
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
        return collectCategoryMapper.deleteCollectCategoryByIds(categoryIds);
    }
}

