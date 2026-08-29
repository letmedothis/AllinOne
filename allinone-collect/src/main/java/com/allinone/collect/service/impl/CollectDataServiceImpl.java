package com.allinone.collect.service.impl;

import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.poi.ExcelUtil;
import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectDataCell;
import com.allinone.collect.mapper.CollectDataMapper;
import com.allinone.collect.mapper.CollectDataCellMapper;
import com.allinone.collect.service.ICollectDataService;
import com.allinone.collect.service.IDataWriteBackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.math.BigDecimal;
import java.util.Objects;

@Service
public class CollectDataServiceImpl implements ICollectDataService {

    private static final Logger log = LoggerFactory.getLogger(CollectDataServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 导出汇总表工作表名 */
    private static final String EXPORT_SUMMARY_SHEET_NAME = "填报记录";
    /** 单次导出的记录数上限，防止一次性构建过多工作表拖垮内存 */
    private static final int EXPORT_MAX_RECORDS = 200;
    /** 单条记录单元格快照数上限，超过则跳过该记录的工作表并在汇总表备注说明 */
    private static final int EXPORT_MAX_CELLS_PER_RECORD = 50000;
    /** POI 限制的工作表名最大长度 */
    private static final int EXPORT_MAX_SHEET_NAME_LENGTH = 31;

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

    /**
     * 构建导出工作簿：
     * 第一个工作表“填报记录”沿用 ExcelUtil&lt;CollectData&gt; 的 @Excel 列（元数据汇总）；
     * 之后为筛选结果中的每条填报记录按 collect_data_cell 快照重建值网格追加工作表，
     * 工作表名为“序号-模板编码”（Excel 非法字符替换为下划线并截断到 31 字符）。
     * 记录无快照时跳过并在汇总表“备注”列说明“无填报内容”；
     * 单条记录快照超过 {@link #EXPORT_MAX_CELLS_PER_RECORD} 时同样跳过并备注“内容过大未导出”，防止构建超大网格占用过多内存。
     * 多 Sheet 记录按 sheet_index 分组，每组生成一个独立工作表（名称追加“-S{sheetIndex}”），
     * 相比用空行分隔语义更清晰，查看方可直接按原模板 Sheet 对照。
     */
    @Override
    public SXSSFWorkbook exportWorkbook(CollectData query) {
        List<CollectData> list = selectCollectDataList(query);
        if (list.size() > EXPORT_MAX_RECORDS) {
            throw new ServiceException("导出数据过多，请缩小筛选范围");
        }

        // 一次性批量取回全部单元格快照并按 data_id 分组，避免逐条记录查询
        Map<Long, List<CollectDataCell>> cellsByDataId = loadCellsByDataId(list);

        // 先确定每条记录的备注，再写汇总表，保证“备注”列同步导出
        for (CollectData record : list) {
            List<CollectDataCell> cells = cellsByDataId.get(record.getDataId());
            if (cells == null || cells.isEmpty()) {
                record.setExportNote("无填报内容");
            } else if (cells.size() > EXPORT_MAX_CELLS_PER_RECORD) {
                record.setExportNote("内容过大未导出");
            }
        }

        SXSSFWorkbook wb = new SXSSFWorkbook(500);
        boolean success = false;
        try {
            // 第一个工作表：元数据汇总（无数据时仅有此表）
            ExcelUtil<CollectData> util = new ExcelUtil<>(CollectData.class);
            util.initWithWorkbook(wb, list, EXPORT_SUMMARY_SHEET_NAME, "");
            util.writeSheet();

            Set<String> usedSheetNames = new HashSet<>();
            usedSheetNames.add(EXPORT_SUMMARY_SHEET_NAME);
            int seq = 0;
            for (CollectData record : list) {
                seq++;
                List<CollectDataCell> cells = cellsByDataId.get(record.getDataId());
                if (cells == null || cells.isEmpty() || cells.size() > EXPORT_MAX_CELLS_PER_RECORD) {
                    // 已在汇总表“备注”列说明，不生成工作表
                    continue;
                }
                appendDataSheets(wb, record, cells, seq, usedSheetNames);
            }
            success = true;
            return wb;
        } finally {
            // 构建中途失败时释放已创建的工作簿，避免临时文件残留
            if (!success) {
                IOUtils.closeQuietly(wb);
                wb.dispose();
            }
        }
    }

    /** 批量查询单元格快照并按 data_id 分组 */
    private Map<Long, List<CollectDataCell>> loadCellsByDataId(List<CollectData> list) {
        Map<Long, List<CollectDataCell>> result = new HashMap<>();
        if (list.isEmpty()) {
            return result;
        }
        List<Long> dataIds = new ArrayList<>(list.size());
        for (CollectData record : list) {
            dataIds.add(record.getDataId());
        }
        for (CollectDataCell cell : collectDataCellMapper.selectCollectDataCellByDataIds(dataIds)) {
            result.computeIfAbsent(cell.getDataId(), key -> new ArrayList<>()).add(cell);
        }
        return result;
    }

    /**
     * 按单元格快照重建值网格并追加为工作表：行=快照最大 row_index，列=快照最大 col_index，值填 cell_value（空值留白）。
     * 使用 TreeMap 按行号、列号排序，保证行按递增顺序创建（SXSSF 要求行号递增）。
     */
    private void appendDataSheets(SXSSFWorkbook wb, CollectData record, List<CollectDataCell> cells, int seq, Set<String> usedSheetNames) {
        Map<Integer, List<CollectDataCell>> cellsBySheetIndex = new TreeMap<>();
        for (CollectDataCell cell : cells) {
            int sheetIndex = cell.getSheetIndex() == null ? 0 : cell.getSheetIndex();
            cellsBySheetIndex.computeIfAbsent(sheetIndex, key -> new ArrayList<>()).add(cell);
        }
        boolean multiSheet = cellsBySheetIndex.size() > 1;
        for (Map.Entry<Integer, List<CollectDataCell>> sheetEntry : cellsBySheetIndex.entrySet()) {
            String sheetName = resolveDataSheetName(seq, record.getTemplateCode(), multiSheet ? sheetEntry.getKey() : null, usedSheetNames);
            Sheet sheet = wb.createSheet(sheetName);
            Map<Integer, Map<Integer, String>> grid = new TreeMap<>();
            for (CollectDataCell cell : sheetEntry.getValue()) {
                int rowIndex = cell.getRowIndex() == null ? 0 : cell.getRowIndex();
                int colIndex = cell.getColIndex() == null ? 0 : cell.getColIndex();
                grid.computeIfAbsent(rowIndex, key -> new TreeMap<>()).put(colIndex, cell.getCellValue());
            }
            for (Map.Entry<Integer, Map<Integer, String>> rowEntry : grid.entrySet()) {
                Row row = sheet.createRow(rowEntry.getKey());
                for (Map.Entry<Integer, String> cellEntry : rowEntry.getValue().entrySet()) {
                    String value = cellEntry.getValue();
                    if (value == null) {
                        continue;
                    }
                    Cell cell = row.createCell(cellEntry.getKey());
                    cell.setCellValue(value);
                }
            }
        }
    }

    /**
     * 生成数据工作表名：“序号-模板编码”（多 Sheet 记录追加“-S{sheetIndex}”）。
     * Excel 非法字符（: \\ / ? * [ ] 及控制字符）替换为下划线并截断到 31 字符；
     * 截断或重名时在尾部追加“-n”保证工作表名唯一。
     */
    private String resolveDataSheetName(int seq, String templateCode, Integer sourceSheetIndex, Set<String> usedSheetNames) {
        String code = StringUtils.isNotBlank(templateCode) ? templateCode : "未知模板";
        StringBuilder name = new StringBuilder().append(seq).append('-').append(code);
        if (sourceSheetIndex != null) {
            name.append("-S").append(sourceSheetIndex);
        }
        String sanitized = name.toString().replaceAll("[\\\\/:*?\\[\\]\\x00-\\x1F]", "_");
        if (sanitized.length() > EXPORT_MAX_SHEET_NAME_LENGTH) {
            sanitized = sanitized.substring(0, EXPORT_MAX_SHEET_NAME_LENGTH);
        }
        String candidate = sanitized;
        int tail = 2;
        while (!usedSheetNames.add(candidate)) {
            String suffix = "-" + tail++;
            int keep = Math.min(sanitized.length(), EXPORT_MAX_SHEET_NAME_LENGTH - suffix.length());
            candidate = sanitized.substring(0, keep) + suffix;
        }
        return candidate;
    }

    @SuppressWarnings("unchecked")
    protected List<CollectDataCell> parseLuckysheetJson(String formData) {
        List<CollectDataCell> result = new ArrayList<>();
        if (formData == null || formData.isEmpty()) return result;
        try {
            List<Map<String, Object>> root = MAPPER.readValue(formData, List.class);
            if (root.isEmpty()) return result;
            // 平铺单元格列表的首个元素含 r/c；工作簿（多 sheet）首元素为 sheet 对象。
            // 以此判定，避免首个工作表缺失 celldata 时把全部 sheet 塌缩进 sheet 0。
            boolean flatCellList = root.get(0).containsKey("r") || root.get(0).containsKey("c");
            if (!flatCellList) {
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
