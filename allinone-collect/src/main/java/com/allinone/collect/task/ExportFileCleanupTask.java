package com.allinone.collect.task;

import com.allinone.common.config.RuoYiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.regex.Pattern;

/**
 * 异步导出文件定期清理（功能模块设计 §1.5：导出文件 24 小时后自动清理）。
 * 下载目录由多个组件共用，只删除异步导出任务的命名模式文件
 * （{uuid}_填报数据_{yyyyMMddHHmmss}.xlsx，见 CollectExportTaskServiceImpl），避免误删他类文件。
 * 任务记录保留，下载已过期的任务时由下载接口提示“导出文件已失效”。
 */
@Component
public class ExportFileCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(ExportFileCleanupTask.class);

    /** 导出文件保留时长：24 小时 */
    private static final long RETENTION_MILLIS = 24 * 60 * 60 * 1000L;

    /** 异步导出任务生成的文件名模式 */
    private static final Pattern EXPORT_FILE_PATTERN =
            Pattern.compile("^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}_填报数据_\\d{14}\\.xlsx$");

    @Scheduled(cron = "${allinone.collect.export.cleanup-cron:0 30 3 * * ?}")
    public void cleanExpiredExportFiles() {
        File dir = new File(RuoYiConfig.getDownloadPath());
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        long expireBefore = System.currentTimeMillis() - RETENTION_MILLIS;
        int removed = 0;
        for (File file : files) {
            if (file.isFile()
                    && file.lastModified() < expireBefore
                    && EXPORT_FILE_PATTERN.matcher(file.getName()).matches()
                    && file.delete()) {
                removed++;
            }
        }
        if (removed > 0) {
            log.info("清理过期导出文件 {} 个（目录：{}）", removed, dir.getPath());
        }
    }
}
