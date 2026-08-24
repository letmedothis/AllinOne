package com.allinone.collect.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.collect.domain.CollectCategory;
import com.allinone.collect.service.ICollectCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/collect/category")
public class CollectCategoryController extends BaseController {

    @Autowired
    private ICollectCategoryService collectCategoryService;

    @PreAuthorize("@ss.hasPermi('collect:category:list')")
    @GetMapping("/list")
    public TableDataInfo list(CollectCategory category) {
        startPage();
        List<CollectCategory> list = collectCategoryService.selectCollectCategoryList(category);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('collect:category:add')")
    @Log(title = "填报分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CollectCategory category) {
        int rows = collectCategoryService.insertCollectCategory(category);
        return rows > 0 ? success(category) : error("新增填报分类失败");
    }

    @PreAuthorize("@ss.hasPermi('collect:category:edit')")
    @Log(title = "填报分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CollectCategory category) {
        return toAjax(collectCategoryService.updateCollectCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('collect:category:remove')")
    @Log(title = "填报分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds) {
        return toAjax(collectCategoryService.deleteCollectCategoryByIds(categoryIds));
    }
}

