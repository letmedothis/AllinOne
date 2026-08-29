package com.allinone.collect.service;

import com.allinone.collect.domain.CollectData;
import java.util.List;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

public interface ICollectDataService {
    List<CollectData> selectCollectDataList(CollectData data);
    CollectData selectCollectDataById(Long dataId);
    int insertCollectData(CollectData data);
    int updateCollectData(CollectData data);
    int submitData(Long dataId);
    int deleteCollectDataByIds(Long[] dataIds);

    /**
     * 构建填报数据导出工作簿：第一个工作表为“填报记录”元数据汇总，
     * 之后为每条填报记录按单元格快照（collect_data_cell）重建的值网格工作表。
     *
     * @param query 查询条件（与列表页筛选一致）
     * @return 已写入全部 Sheet 的流式工作簿，调用方负责写出并关闭
     */
    SXSSFWorkbook exportWorkbook(CollectData query);
}

