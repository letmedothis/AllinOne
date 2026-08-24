package com.allinone.collect.service.impl;

import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectDataCell;
import com.allinone.collect.mapper.CollectDataMapper;
import com.allinone.collect.mapper.CollectDataCellMapper;
import com.allinone.collect.service.ICollectDataService;
import com.allinone.collect.service.IDataWriteBackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
public class CollectDataServiceImpl implements ICollectDataService {

    private static final Logger log = LoggerFactory.getLogger(CollectDataServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private CollectDataMapper collectDataMapper;

    @Autowired(required = false)
    private IDataWriteBackService dataWriteBackService;

    @Autowired
    private CollectDataCellMapper collectDataCellMapper;

    @Override
    public List<CollectData> selectCollectDataList(CollectData data) {
        return collectDataMapper.selectCollectDataList(data);
    }

    @Override
    public CollectData selectCollectDataById(Long dataId) {
        return collectDataMapper.selectCollectDataById(dataId);
    }

    @Override
    @Transactional
    public int insertCollectData(CollectData data) {
        data.setCreateTime(DateUtils.getNowDate());
        data.setBizStatus("draft");
        if (data.getDeptId() == null) {
            try { data.setDeptId(SecurityUtils.getLoginUser().getDeptId()); } catch (Exception e) {
                log.warn("获取当前用户部门ID失败，dataId={}", data.getDataId(), e);
            }
        }
        return collectDataMapper.insertCollectData(data);
    }

    @Override
    @Transactional
    public int updateCollectData(CollectData data) {
        data.setUpdateTime(DateUtils.getNowDate());
        return collectDataMapper.updateCollectData(data);
    }

    @Override
    @Transactional
    public int submitData(Long dataId) {
        CollectData data = collectDataMapper.selectCollectDataById(dataId);
        if (data == null) return 0;

        // Tier 2: 解析表单 JSON 写入 collect_data_cell（供 JimuReport SQL 查询）
        try {
            List<CollectDataCell> cells = parseLuckysheetJson(data.getFormData());
            if (!cells.isEmpty()) {
                cells.forEach(c -> { c.setDataId(dataId); c.setTemplateId(data.getTemplateId()); });
                collectDataCellMapper.batchUpsert(cells);
            }
        } catch (Exception e) {
            log.warn("单元格解析失败 dataId={}", dataId, e);
        }

        // Tier 3: 字段映射回写业务表
        if (dataWriteBackService != null) {
            try { dataWriteBackService.writeBack(data); } catch (Exception e) {
                log.warn("数据回写失败 dataId={}", dataId, e);
            }
        }

        data.setBizStatus("submitted");
        data.setSubmitBy(SecurityUtils.getUsername());
        data.setSubmitTime(DateUtils.getNowDate());
        return collectDataMapper.updateCollectDataStatus(data);
    }

    @Override
    public int deleteCollectDataByIds(Long[] dataIds) {
        return collectDataMapper.deleteCollectDataByIds(dataIds);
    }

    private List<CollectDataCell> parseLuckysheetJson(String formData) {
        List<CollectDataCell> result = new ArrayList<>();
        if (formData == null || formData.isEmpty()) return result;
        try {
            List<Map<String, Object>> cells = MAPPER.readValue(formData, List.class);
            for (Map<String, Object> cell : cells) {
                int r = ((Number) cell.getOrDefault("r", 0)).intValue();
                int c = ((Number) cell.getOrDefault("c", 0)).intValue();
                Object vObj = cell.get("v");
                String cellText = "", cellType = "string";
                if (vObj instanceof Map) {
                    Map v = (Map) vObj;
                    cellText = v.get("m") != null ? v.get("m").toString() : (v.get("v") != null ? v.get("v").toString() : "");
                    cellType = v.get("ct") != null ? v.get("ct").toString() : "string";
                }
                CollectDataCell dc = new CollectDataCell();
                dc.setRowIndex(r); dc.setColIndex(c);
                dc.setCellText(cellText); dc.setCellType(cellType);
                dc.setSheetIndex(0);
                result.add(dc);
            }
        } catch (Exception e) {
            log.warn("Luckysheet JSON解析失败 formData长度={}", formData != null ? formData.length() : 0, e);
        }
        return result;
    }
}
