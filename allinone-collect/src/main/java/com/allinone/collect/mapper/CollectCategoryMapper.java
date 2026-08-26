package com.allinone.collect.mapper;

import com.allinone.collect.domain.CollectCategory;
import java.util.List;

public interface CollectCategoryMapper {
    List<CollectCategory> selectCollectCategoryList(CollectCategory category);
    CollectCategory selectCollectCategoryById(Long categoryId);
    List<CollectCategory> selectCollectCategoryByParentId(Long parentId);
    int insertCollectCategory(CollectCategory category);
    int updateCollectCategory(CollectCategory category);
    int deleteCollectCategoryById(Long categoryId);
    int deleteCollectCategoryByIds(Long[] categoryIds);
    int countTemplateByCategoryIds(Long[] categoryIds);
}

