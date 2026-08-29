package com.allinone.collect.controller;

import com.allinone.common.annotation.Log;
import com.allinone.common.annotation.RepeatSubmit;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.collect.service.ICollectDataService;
import com.allinone.collect.domain.CollectData;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
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

    /**
     * 导出填报数据：第一个工作表为“填报记录”元数据汇总，
     * 之后每条填报记录按单元格快照重建一个值网格工作表（详见 CollectDataServiceImpl#exportWorkbook）。
     */
    @PreAuthorize("@ss.hasPermi('collect:data:export')")
    @Log(title = "填报数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectData data) {
        SXSSFWorkbook wb = collectDataService.exportWorkbook(data);
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            wb.write(response.getOutputStream());
        } catch (Exception e) {
            logger.error("导出填报数据异常{}", e.getMessage());
        } finally {
            IOUtils.closeQuietly(wb);
            wb.dispose();
        }
    }

    @PreAuthorize("@ss.hasPermi('collect:data:query')")
    @GetMapping("/{dataId}")
    public AjaxResult getInfo(@PathVariable Long dataId) {
        return success(collectDataService.selectCollectDataById(dataId));
    }

    @PreAuthorize("@ss.hasPermi('collect:data:add')")
    // 请求体含整本工作簿 JSON（自动保存高频触发），不落库到操作日志
    @Log(title = "填报数据", businessType = BusinessType.INSERT, isSaveRequestData = false)
    @PostMapping
    public AjaxResult add(@RequestBody CollectData data) {
        int rows = collectDataService.insertCollectData(data);
        if (rows > 0) {
            // 大体积 formData 不回传，前端保留刚提交的本地副本
            data.setFormData(null);
            return success(data);
        }
        return error("新增填报数据失败");
    }

    @PreAuthorize("@ss.hasPermi('collect:data:edit')")
    // 请求体含整本工作簿 JSON（自动保存高频触发），不落库到操作日志
    @Log(title = "填报数据", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    @PutMapping
    public AjaxResult edit(@RequestBody CollectData data) {
        int rows = collectDataService.updateCollectData(data);
        if (rows > 0) {
            CollectData updated = collectDataService.selectCollectDataById(data.getDataId());
            // 大体积 formData 不回传，避免 30s 一次的自动保存产生双向全量传输
            updated.setFormData(null);
            return success(updated);
        }
        return error("修改填报数据失败");
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

