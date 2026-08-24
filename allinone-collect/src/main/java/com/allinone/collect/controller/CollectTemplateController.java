package com.allinone.collect.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.collect.domain.CollectTemplate;
import com.allinone.collect.service.ICollectTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/collect/template")
public class CollectTemplateController extends BaseController {

    @Autowired
    private ICollectTemplateService collectTemplateService;

    @PreAuthorize("@ss.hasPermi('collect:template:list')")
    @GetMapping("/list")
    public TableDataInfo list(CollectTemplate template) {
        startPage();
        List<CollectTemplate> list = collectTemplateService.selectCollectTemplateList(template);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('collect:template:query')")
    @GetMapping("/{templateId}")
    public AjaxResult getInfo(@PathVariable Long templateId) {
        return success(collectTemplateService.selectCollectTemplateById(templateId));
    }

    @PreAuthorize("@ss.hasPermi('collect:template:add')")
    @Log(title = "填报模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CollectTemplate template) {
        return toAjax(collectTemplateService.insertCollectTemplate(template));
    }

    @PreAuthorize("@ss.hasPermi('collect:template:edit')")
    @Log(title = "填报模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CollectTemplate template) {
        return toAjax(collectTemplateService.updateCollectTemplate(template));
    }

    @PreAuthorize("@ss.hasPermi('collect:template:remove')")
    @Log(title = "填报模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{templateIds}")
    public AjaxResult remove(@PathVariable Long[] templateIds) {
        return toAjax(collectTemplateService.deleteCollectTemplateByIds(templateIds));
    }

    @PreAuthorize("@ss.hasPermi('collect:template:edit')")
    @Log(title = "填报模板", businessType = BusinessType.UPDATE)
    @PostMapping("/{templateId}/publish")
    public AjaxResult publish(@PathVariable Long templateId, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return toAjax(collectTemplateService.updateStatus(templateId, status));
    }
}

