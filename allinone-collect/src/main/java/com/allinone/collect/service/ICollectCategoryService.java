package com.allinone.collect.service;

import com.allinone.collect.domain.CollectCategory;
import java.util.List;

public interface ICollectCategoryService {
    List<CollectCategory> selectCollectCategoryList(CollectCategory category);
    CollectCategory selectCollectCategoryById(Long categoryId);
    int insertCollectCategory(CollectCategory category);
    int updateCollectCategory(CollectCategory category);
    int deleteCollectCategoryByIds(Long[] categoryIds);
}

