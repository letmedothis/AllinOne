package com.allinone.collect.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.annotation.RepeatSubmit;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.collect.domain.CollectData;
import com.allinone.collect.service.ICollectDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/collect/data")
public class CollectDataController extends BaseController {

    @Autowired
    private ICollectDataService collectDataService;

    @PreAuthorize("@ss.hasPermi('collect:data:list')")
    @GetMapping("/list")
    public TableDataInfo list(CollectData data) {
        startPage();
        List<CollectData> list = collectDataService.selectCollectDataList(data);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('collect:data:query')")
    @GetMapping("/{dataId}")
    public AjaxResult getInfo(@PathVariable Long dataId) {
        return success(collectDataService.selectCollectDataById(dataId));
    }

    @PreAuthorize("@ss.hasPermi('collect:data:add')")
    @Log(title = "填报数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CollectData data) {
        return toAjax(collectDataService.insertCollectData(data));
    }

    @PreAuthorize("@ss.hasPermi('collect:data:edit')")
    @Log(title = "填报数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CollectData data) {
        return toAjax(collectDataService.updateCollectData(data));
    }

    @PreAuthorize("@ss.hasPermi('collect:data:remove')")
    @Log(title = "填报数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dataIds}")
    public AjaxResult remove(@PathVariable Long[] dataIds) {
        return toAjax(collectDataService.deleteCollectDataByIds(dataIds));
    }

    @PreAuthorize("@ss.hasPermi('collect:data:edit')")
    @Log(title = "填报数据", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{dataId}/submit")
    public AjaxResult submit(@PathVariable Long dataId) {
        return toAjax(collectDataService.submitData(dataId));
    }
}

