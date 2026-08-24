package com.allinone.collect.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.collect.domain.CollectFieldMapping;
import com.allinone.collect.service.ICollectFieldMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/collect/mapping")
public class CollectFieldMappingController extends BaseController {

    @Autowired
    private ICollectFieldMappingService collectFieldMappingService;

    @PreAuthorize("@ss.hasPermi('collect:mapping:list')")
    @GetMapping("/list")
    public TableDataInfo list(CollectFieldMapping mapping) {
        startPage();
        List<CollectFieldMapping> list = collectFieldMappingService.selectCollectFieldMappingList(mapping);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('collect:mapping:query')")
    @GetMapping("/{mappingId}")
    public AjaxResult getInfo(@PathVariable Long mappingId) {
        return success(collectFieldMappingService.selectCollectFieldMappingById(mappingId));
    }

    @PreAuthorize("@ss.hasPermi('collect:mapping:add')")
    @Log(title = "字段映射", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CollectFieldMapping mapping) {
        return toAjax(collectFieldMappingService.insertCollectFieldMapping(mapping));
    }

    @PreAuthorize("@ss.hasPermi('collect:mapping:edit')")
    @Log(title = "字段映射", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CollectFieldMapping mapping) {
        return toAjax(collectFieldMappingService.updateCollectFieldMapping(mapping));
    }

    @PreAuthorize("@ss.hasPermi('collect:mapping:remove')")
    @Log(title = "字段映射", businessType = BusinessType.DELETE)
    @DeleteMapping("/{mappingIds}")
    public AjaxResult remove(@PathVariable Long[] mappingIds) {
        return toAjax(collectFieldMappingService.deleteCollectFieldMappingByIds(mappingIds));
    }
}
