package com.allinone.collect.mapper;

import com.allinone.collect.domain.CollectExportTask;
import java.util.List;

public interface CollectExportTaskMapper {
    CollectExportTask selectCollectExportTaskById(Long taskId);
    List<CollectExportTask> selectCollectExportTaskList(CollectExportTask task);
    int insertCollectExportTask(CollectExportTask task);
    int updateCollectExportTask(CollectExportTask task);
}
