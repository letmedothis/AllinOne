package com.allinone.collect.service;

import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectExportTask;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public interface ICollectExportTaskService {

    /** 创建异步导出任务并提交后台执行，返回任务 ID */
    Long createTask(CollectData query);

    /** 查询导出任务列表（非管理员仅可见自己的任务） */
    List<CollectExportTask> selectExportTaskList(CollectExportTask query);

    /** 下载任务生成的文件（校验任务属主） */
    void download(Long taskId, HttpServletResponse response);
}
