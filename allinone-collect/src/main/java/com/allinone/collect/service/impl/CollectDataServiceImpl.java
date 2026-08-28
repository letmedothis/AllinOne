package com.allinone.collect.service.impl;

import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.common.exception.ServiceException;
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
import java.math.BigDecimal;
import java.util.Objects;

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
        if (!currentUserIsAdmin()) {
            data.setCreateBy(currentUsername());
        }
        return collectDataMapper.selectCollectDataList(data);
    }

    @Override
    public CollectData selectCollectDataById(Long dataId) {
        CollectData data = collectDataMapper.selectCollectDataById(dataId);
        requireOwner(data);
        return data;
    }

    @Override
    @Transactional
    public int insertCollectData(CollectData data) {
        data.setDataId(IdUtils.nextLongId());
        data.setCreateTime(DateUtils.getNowDate());
        data.setCreateBy(currentUsername());
        data.setBizStatus("draft");
        data.setVersion(1);
        data.setDeptId(currentDeptId());
        data.setSubmitBy(null);
        data.setSubmitTime(null);
        return collectDataMapper.insertCollectData(data);
    }

    @Override
    @Transactional
    public int updateCollectData(CollectData data) {
        if (data.getDataId() == null || data.getVersion() == null) {
            throw new ServiceException("缺少填报数据ID或版本号");
        }
        CollectData existing = collectDataMapper.selectCollectDataById(data.getDataId());
        requireOwner(existing);
        requireDraft(existing);
        data.setBizStatus(null);
        data.setUpdateTime(DateUtils.getNowDate());
        data.setUpdateBy(currentUsername());
        int rows = collectDataMapper.updateCollectData(data);
        if (rows == 0) {
            throw new ServiceException("填报数据已被其他用户修改，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional
    public int submitData(Long dataId) {
        CollectData data = collectDataMapper.selectCollectDataById(dataId);
        requireOwner(data);
        requireDraft(data);

        // Tier 2: 解析表单 JSON 写入 collect_data_cell（供 JimuReport SQL 查询）
        List<CollectDataCell> cells = parseLuckysheetJson(data.getFormData());
        // collect_data_cell 是 form_data 的完整查询快照，不是只追加变更的日志。
        // 先清理旧快照，确保前端已删除或清空的单元格不会在报表查询中残留。
        collectDataCellMapper.deleteCollectDataCellByDataId(dataId);
        if (!cells.isEmpty()) {
            for (CollectDataCell cell : cells) {
                cell.setCellId(IdUtils.nextLongId());
                cell.setDataId(dataId);
                cell.setTemplateId(data.getTemplateId());
                cell.setCreateBy(currentUsername());
                cell.setCreateTime(DateUtils.getNowDate());
            }
            for (int from = 0; from < cells.size(); from += 500) {
                collectDataCellMapper.batchUpsert(cells.subList(from, Math.min(from + 500, cells.size())));
            }
        }

        // Tier 3: 字段映射回写业务表
        if (dataWriteBackService != null) {
            dataWriteBackService.writeBack(data);
        }

        data.setBizStatus("submitted");
        data.setSubmitBy(currentUsername());
        data.setSubmitTime(DateUtils.getNowDate());
        data.setUpdateTime(DateUtils.getNowDate());
        int rows = collectDataMapper.updateCollectDataStatus(data);
        if (rows == 0) {
            throw new ServiceException("填报状态已变化，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteCollectDataByIds(Long[] dataIds) {
        for (Long dataId : dataIds) {
            CollectData data = collectDataMapper.selectCollectDataById(dataId);
            requireOwner(data);
            requireDraft(data);
        }
        int rows = collectDataMapper.deleteCollectDataByIds(dataIds);
        if (rows != dataIds.length) {
            throw new ServiceException("部分填报数据状态已变化，请刷新后重试");
        }
        for (Long dataId : dataIds) {
            collectDataCellMapper.deleteCollectDataCellByDataId(dataId);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    protected List<CollectDataCell> parseLuckysheetJson(String formData) {
        List<CollectDataCell> result = new ArrayList<>();
        if (formData == null || formData.isEmpty()) return result;
        try {
            List<Map<String, Object>> root = MAPPER.readValue(formData, List.class);
            if (root.isEmpty()) return result;
            if (root.get(0).containsKey("celldata")) {
                for (int sheetIndex = 0; sheetIndex < root.size(); sheetIndex++) {
                    Object rawCells = root.get(sheetIndex).get("celldata");
                    if (rawCells instanceof List) {
                        appendCells(result, (List<Map<String, Object>>) rawCells, sheetIndex);
                    }
                }
            } else {
                appendCells(result, root, 0);
            }
        } catch (Exception e) {
            throw new ServiceException("Luckysheet JSON解析失败").setDetailMessage(e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void appendCells(List<CollectDataCell> result, List<Map<String, Object>> cells, int sheetIndex) {
        for (Map<String, Object> cell : cells) {
            Object row = cell.get("r");
            Object column = cell.get("c");
            if (!(row instanceof Number) || !(column instanceof Number)) continue;
                int r = ((Number) cell.getOrDefault("r", 0)).intValue();
                int c = ((Number) cell.getOrDefault("c", 0)).intValue();
                Object vObj = cell.get("v");
                String cellText = "", cellValue = "", cellType = "string", formula = null;
                BigDecimal numericValue = null;
                if (vObj instanceof Map) {
                    Map<String, Object> v = (Map<String, Object>) vObj;
                    cellText = v.get("m") != null ? v.get("m").toString() : (v.get("v") != null ? v.get("v").toString() : "");
                    Object rawValue = v.get("v");
                    cellValue = rawValue != null ? rawValue.toString() : "";
                    if (rawValue instanceof Number) numericValue = new BigDecimal(rawValue.toString());
                    Object ct = v.get("ct");
                    cellType = ct instanceof Map && ((Map<?, ?>) ct).get("t") != null
                            ? ((Map<?, ?>) ct).get("t").toString() : "string";
                    formula = v.get("f") != null ? v.get("f").toString() : null;
                } else if (vObj != null) {
                    cellText = vObj.toString();
                    cellValue = cellText;
                    if (vObj instanceof Number) numericValue = new BigDecimal(vObj.toString());
                }
                CollectDataCell dc = new CollectDataCell();
                dc.setRowIndex(r); dc.setColIndex(c);
                dc.setCellText(cellText); dc.setCellType(cellType);
                dc.setCellValue(cellValue); dc.setCellNumericValue(numericValue);
                dc.setSheetIndex(sheetIndex);
                dc.setIsFormula(formula == null ? "0" : "1");
                dc.setFormulaExpr(formula);
                result.add(dc);
        }
    }

    private void requireOwner(CollectData data) {
        if (data == null) throw new ServiceException("填报数据不存在");
        if (!currentUserIsAdmin() && !Objects.equals(data.getCreateBy(), currentUsername())) {
            throw new ServiceException("无权访问该填报数据");
        }
    }

    private void requireDraft(CollectData data) {
        if (!"draft".equals(data.getBizStatus())) {
            throw new ServiceException("已提交的数据不能再次修改或提交");
        }
    }

    protected String currentUsername() {
        return SecurityUtils.getUsername();
    }

    protected Long currentDeptId() {
        return SecurityUtils.getDeptId();
    }

    protected boolean currentUserIsAdmin() {
        return SecurityUtils.getLoginUser().getUser().isAdmin();
    }
}
