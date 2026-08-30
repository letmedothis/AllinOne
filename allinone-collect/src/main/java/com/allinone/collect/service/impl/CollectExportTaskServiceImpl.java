package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectExportTask;
import com.allinone.collect.mapper.CollectExportTaskMapper;
import com.allinone.collect.service.ICollectDataService;
import com.allinone.collect.service.ICollectExportTaskService;
import com.allinone.common.config.RuoYiConfig;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.file.FileUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Service;

/**
 * 填报数据异步导出：任务落库后交线程池后台生成文件，前端轮询任务状态后按任务 ID 下载。
 * 大导出不再占用 HTTP 请求线程与浏览器连接，也不会因网关/浏览器超时中断。
 */
@Service
public class CollectExportTaskServiceImpl implements ICollectExportTaskService {

    private static final Logger log = LoggerFactory.getLogger(CollectExportTaskServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** error_msg 列长度上限 */
    private static final int MAX_ERROR_MSG_LENGTH = 500;

    @Autowired
    private CollectExportTaskMapper exportTaskMapper;

    @Autowired
    private ICollectDataService collectDataService;

    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public Long createTask(CollectData query) {
        CollectExportTask task = new CollectExportTask();
        task.setTaskId(IdUtils.nextLongId());
        task.setTaskName("填报数据导出 " + DateUtils.dateTimeNow());
        try {
            task.setQueryJson(MAPPER.writeValueAsString(query));
        } catch (Exception e) {
            throw new ServiceException("导出条件序列化失败").setDetailMessage(e.getMessage());
        }
        task.setStatus("pending");
        task.setCreateBy(currentUsername());
        task.setCreateTime(DateUtils.getNowDate());
        exportTaskMapper.insertCollectExportTask(task);

        // 携带提交时刻的 SecurityContext：exportWorkbook 内部按当前用户过滤可见数据
        taskExecutor.execute(new DelegatingSecurityContextRunnable(() -> runTask(task.getTaskId())));
        log.info("创建异步导出任务 taskId={} user={}", task.getTaskId(), task.getCreateBy());
        return task.getTaskId();
    }

    /** 后台执行导出。包级私有以便单测；仅处理 pending 状态，重复调度幂等跳过。 */
    void runTask(Long taskId) {
        CollectExportTask task = exportTaskMapper.selectCollectExportTaskById(taskId);
        if (task == null || !"pending".equals(task.getStatus())) {
            return;
        }
        Date now = DateUtils.getNowDate();
        task.setStatus("running");
        task.setUpdateTime(now);
        exportTaskMapper.updateCollectExportTask(task);

        SXSSFWorkbook wb = null;
        boolean success = false;
        try {
            CollectData query = MAPPER.readValue(task.getQueryJson(), CollectData.class);
            wb = collectDataService.exportWorkbook(query);
            File downloadDir = new File(RuoYiConfig.getDownloadPath());
            if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                throw new ServiceException("创建导出目录失败: " + downloadDir);
            }
            String fileName = IdUtils.fastUUID() + "_填报数据_" + DateUtils.dateTimeNow() + ".xlsx";
            File target = new File(downloadDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(target)) {
                wb.write(fos);
            }
            task.setStatus("success");
            task.setFileName(fileName);
            task.setFinishTime(DateUtils.getNowDate());
            success = true;
        } catch (Exception e) {
            log.error("异步导出任务失败 taskId={}", taskId, e);
            task.setStatus("failed");
            task.setErrorMsg(StringUtils.abbreviate(e.getMessage(), MAX_ERROR_MSG_LENGTH));
            task.setFinishTime(DateUtils.getNowDate());
        } finally {
            if (wb != null) {
                IOUtils.closeQuietly(wb);
                wb.dispose();
            }
            exportTaskMapper.updateCollectExportTask(task);
        }
        if (success) {
            log.info("异步导出任务完成 taskId={} file={}", taskId, task.getFileName());
        }
    }

    @Override
    public List<CollectExportTask> selectExportTaskList(CollectExportTask query) {
        // 与填报数据列表一致：非管理员只能看到自己创建的任务
        if (!currentUserIsAdmin()) {
            query.setCreateBy(currentUsername());
        }
        return exportTaskMapper.selectCollectExportTaskList(query);
    }

    @Override
    public void download(Long taskId, HttpServletResponse response) {
        CollectExportTask task = exportTaskMapper.selectCollectExportTaskById(taskId);
        requireOwner(task);
        if (!"success".equals(task.getStatus()) || StringUtils.isEmpty(task.getFileName())) {
            throw new ServiceException("任务尚未完成或已失败，无法下载");
        }
        File file = new File(RuoYiConfig.getDownloadPath(), task.getFileName());
        if (!file.exists()) {
            throw new ServiceException("导出文件已失效，请重新导出");
        }
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            FileUtils.setAttachmentResponseHeader(response, task.getFileName());
            FileUtils.writeBytes(file.getAbsolutePath(), response.getOutputStream());
        } catch (Exception e) {
            log.error("下载导出文件失败 taskId={}", taskId, e);
            throw new ServiceException("下载导出文件失败");
        }
    }

    private void requireOwner(CollectExportTask task) {
        if (task == null) {
            throw new ServiceException("导出任务不存在");
        }
        if (!currentUserIsAdmin() && !StringUtils.equals(task.getCreateBy(), currentUsername())) {
            throw new ServiceException("无权访问该导出任务");
        }
    }

    protected String currentUsername() {
        return SecurityUtils.getUsername();
    }

    protected boolean currentUserIsAdmin() {
        return SecurityUtils.getLoginUser().getUser().isAdmin();
    }
}
