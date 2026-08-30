package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectExportTask;
import com.allinone.collect.mapper.CollectExportTaskMapper;
import com.allinone.collect.service.ICollectDataService;
import com.allinone.common.config.RuoYiConfig;
import com.allinone.common.exception.ServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectExportTaskServiceImplTest {

    private final CollectExportTaskMapper taskMapper = mock(CollectExportTaskMapper.class);
    private final ICollectDataService collectDataService = mock(ICollectDataService.class);
    private final ThreadPoolTaskExecutor taskExecutor = mock(ThreadPoolTaskExecutor.class);
    private CollectExportTaskServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new TestableCollectExportTaskService();
        ReflectionTestUtils.setField(service, "exportTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "collectDataService", collectDataService);
        ReflectionTestUtils.setField(service, "taskExecutor", taskExecutor);
        // RuoYiConfig 字段为 static、setter 为实例方法，测试中指向临时目录
        new RuoYiConfig().setProfile(tempDir.toString());
    }

    @Test
    void createTaskPersistsPendingTaskAndSchedulesRun() {
        CollectData query = new CollectData();
        query.setTemplateName("月度报表");

        Long taskId = service.createTask(query);

        assertThat(taskId).isPositive();
        ArgumentCaptor<CollectExportTask> captor = ArgumentCaptor.forClass(CollectExportTask.class);
        verify(taskMapper).insertCollectExportTask(captor.capture());
        CollectExportTask inserted = captor.getValue();
        assertThat(inserted.getTaskId()).isEqualTo(taskId);
        assertThat(inserted.getStatus()).isEqualTo("pending");
        assertThat(inserted.getTaskName()).contains("填报数据导出");
        assertThat(inserted.getCreateBy()).isEqualTo("alice");
        assertThat(inserted.getQueryJson()).contains("月度报表");
        // 任务已提交线程池执行
        verify(taskExecutor).execute(any(Runnable.class));
    }

    @Test
    void runTaskSuccessWritesFileAndMarksSuccess() throws Exception {
        CollectExportTask pending = pendingTask();
        when(taskMapper.selectCollectExportTaskById(1L)).thenReturn(pending);
        SXSSFWorkbook wb = new SXSSFWorkbook(10);
        wb.createSheet("sheet1");
        when(collectDataService.exportWorkbook(any(CollectData.class))).thenReturn(wb);

        service.runTask(1L);

        assertThat(pending.getStatus()).isEqualTo("success");
        assertThat(pending.getFileName()).isNotBlank();
        assertThat(pending.getErrorMsg()).isNull();
        Path file = tempDir.resolve("download").resolve(pending.getFileName());
        assertThat(Files.exists(file)).isTrue();
        assertThat(Files.size(file)).isPositive();
    }

    @Test
    void runTaskFailureMarksFailedWithErrorMsg() {
        CollectExportTask pending = pendingTask();
        when(taskMapper.selectCollectExportTaskById(1L)).thenReturn(pending);
        when(collectDataService.exportWorkbook(any(CollectData.class)))
            .thenThrow(new ServiceException("导出数据过多，请缩小筛选范围"));

        service.runTask(1L);

        assertThat(pending.getStatus()).isEqualTo("failed");
        assertThat(pending.getErrorMsg()).contains("导出数据过多");
        assertThat(pending.getFileName()).isNull();
    }

    @Test
    void runTaskSkipsNonPendingTask() {
        CollectExportTask running = pendingTask();
        running.setStatus("running");
        when(taskMapper.selectCollectExportTaskById(1L)).thenReturn(running);

        service.runTask(1L);

        verify(collectDataService, never()).exportWorkbook(any());
        verify(taskMapper, never()).updateCollectExportTask(any());
    }

    private CollectExportTask pendingTask() {
        CollectExportTask task = new CollectExportTask();
        task.setTaskId(1L);
        task.setQueryJson("{}");
        task.setStatus("pending");
        return task;
    }

    private static final class TestableCollectExportTaskService extends CollectExportTaskServiceImpl {
        @Override
        protected String currentUsername() {
            return "alice";
        }

        @Override
        protected boolean currentUserIsAdmin() {
            return false;
        }
    }
}
