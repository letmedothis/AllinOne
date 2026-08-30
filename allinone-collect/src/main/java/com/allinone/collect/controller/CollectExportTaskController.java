package com.allinone.collect.controller;

import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectExportTask;
import com.allinone.collect.service.ICollectExportTaskService;
import com.allinone.common.annotation.Log;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 填报数据异步导出任务：创建/查询/下载。
 * 权限复用 collect:data:export，不新增菜单权限。
 */
@RestController
@RequestMapping("/collect/data/export")
public class CollectExportTaskController extends BaseController {

    @Autowired
    private ICollectExportTaskService exportTaskService;

    /** 创建异步导出任务，返回任务 ID */
    @PreAuthorize("@ss.hasPermi('collect:data:export')")
    @Log(title = "填报数据导出任务", businessType = BusinessType.EXPORT)
    @PostMapping("/tasks")
    public AjaxResult create(CollectData query) {
        return success(exportTaskService.createTask(query));
    }

    /** 任务列表（非管理员仅可见自己的任务），前端轮询展示进度 */
    @PreAuthorize("@ss.hasPermi('collect:data:export')")
    @GetMapping("/tasks")
    public TableDataInfo list(CollectExportTask query) {
        startPage();
        List<CollectExportTask> list = exportTaskService.selectExportTaskList(query);
        return getDataTable(list);
    }

    /** 下载任务生成的文件（仅任务创建者或管理员） */
    @PreAuthorize("@ss.hasPermi('collect:data:export')")
    @GetMapping("/tasks/{taskId}/download")
    public void download(@PathVariable Long taskId, HttpServletResponse response) {
        exportTaskService.download(taskId, response);
    }
}
