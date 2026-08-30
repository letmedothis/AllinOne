package com.allinone.collect.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.common.utils.poi.ExcelUtil;
import com.allinone.collect.domain.CollectTemplate;
import com.allinone.collect.service.ICollectTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;

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

    @PreAuthorize("@ss.hasPermi('collect:template:export')")
    @Log(title = "填报模板", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectTemplate template) {
        List<CollectTemplate> list = collectTemplateService.selectCollectTemplateList(template);
        new ExcelUtil<>(CollectTemplate.class).exportExcel(response, list, "填报模板");
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
        int rows = collectTemplateService.insertCollectTemplate(template);
        return rows > 0 ? success(template) : error("新增填报模板失败");
    }

    @PreAuthorize("@ss.hasPermi('collect:template:edit')")
    @Log(title = "填报模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CollectTemplate template) {
        int rows = collectTemplateService.updateCollectTemplate(template);
        return rows > 0 ? success(collectTemplateService.selectCollectTemplateById(template.getTemplateId())) : error("修改填报模板失败");
    }

    @PreAuthorize("@ss.hasPermi('collect:template:remove')")
    @Log(title = "填报模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{templateIds}")
    public AjaxResult remove(@PathVariable Long[] templateIds) {
        return toAjax(collectTemplateService.deleteCollectTemplateByIds(templateIds));
    }

    /**
     * 复制模板：克隆为未发布新模板，前端复制后一般直接跳转编辑页
     */
    @PreAuthorize("@ss.hasPermi('collect:template:add')")
    @Log(title = "填报模板", businessType = BusinessType.INSERT)
    @PostMapping("/{templateId}/copy")
    public AjaxResult copy(@PathVariable Long templateId) {
        return success(collectTemplateService.copyTemplate(templateId));
    }

    @PreAuthorize("@ss.hasPermi('collect:template:edit')")
    @Log(title = "填报模板", businessType = BusinessType.UPDATE)
    @PostMapping("/{templateId}/publish")
    public AjaxResult publish(@PathVariable Long templateId, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return toAjax(collectTemplateService.updateStatus(templateId, status));
    }
}

