package com.allinone.collect.service.impl;

import com.allinone.collect.constant.CollectErrorCode;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.DictUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.core.domain.entity.SysDictData;
import com.allinone.common.core.domain.entity.SysRole;
import com.allinone.common.utils.poi.ExcelUtil;
import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectDataCell;
import com.allinone.collect.domain.CollectTemplate;
import com.allinone.collect.mapper.CollectDataMapper;
import com.allinone.collect.mapper.CollectDataCellMapper;
import com.allinone.collect.mapper.CollectTemplateMapper;
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
import java.util.LinkedHashSet;
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
    /** 单次导出的记录数上限（总体设计 §1.5：单次导出上限 10 万行） */
    private static final int EXPORT_MAX_RECORDS = 100_000;
    /** 单条记录单元格快照数上限，超过则跳过该记录的工作表并在汇总表备注说明 */
    private static final int EXPORT_MAX_CELLS_PER_RECORD = 50000;
    /** POI 限制的工作表名最大长度 */
    private static final int EXPORT_MAX_SHEET_NAME_LENGTH = 31;

    @Autowired
    private CollectDataMapper collectDataMapper;

    @Autowired
    private CollectTemplateMapper collectTemplateMapper;

    @Autowired(required = false)
    private IDataWriteBackService dataWriteBackService;

    @Autowired
    private CollectDataCellMapper collectDataCellMapper;

    @Override
    public List<CollectData> selectCollectDataList(CollectData data) {
        if (!currentUserIsAdmin()) {
            String scopeSql = buildDataScopeSql();
            if (scopeSql != null) {
                data.getParams().put("dataScopeSql", scopeSql);
            }
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
        // 错误码 1001/1002：模板必须存在且已发布才能新建填报（下架后不能再新增填报）
        CollectTemplate template = collectTemplateMapper.selectCollectTemplateById(data.getTemplateId());
        if (template == null) {
            throw new ServiceException("填报模板不存在或已删除", CollectErrorCode.TEMPLATE_NOT_FOUND);
        }
        if (!"1".equals(template.getStatus())) {
            throw new ServiceException("模板未发布，暂时不能填报", CollectErrorCode.TEMPLATE_NOT_PUBLISHED);
        }
        data.setDataId(IdUtils.nextLongId());
        data.setCreateTime(DateUtils.getNowDate());
        data.setCreateBy(currentUsername());
        data.setBizStatus("draft");
        data.setVersion(1);
        data.setDeptId(currentDeptId());
        data.setTemplateVersion(template.getVersion());
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
            throw new ServiceException("填报数据已被其他用户修改，请刷新后重试", CollectErrorCode.DATA_VERSION_CONFLICT);
        }
        return rows;
    }

    @Override
    @Transactional
    public int submitData(Long dataId) {
        CollectData data = collectDataMapper.selectCollectDataById(dataId);
        requireOwner(data);
        requireDraft(data);
        CollectTemplate template = collectTemplateMapper.selectCollectTemplateById(data.getTemplateId());
        if (template == null) {
            throw new ServiceException("填报模板不存在或已删除", CollectErrorCode.TEMPLATE_NOT_FOUND);
        }

        // Tier 2: 解析表单 JSON 写入 collect_data_cell（供 JimuReport SQL 查询）
        List<CollectDataCell> cells = parseLuckysheetJson(data.getFormData());
        // 提交时按模板数据验证规则兜底校验（必填/字典合法性），防止绕过前端直接调 API
        validateCellRules(cells, template.getTemplateJson());
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
        data.setTemplateVersion(template.getVersion());
        int rows = collectDataMapper.updateCollectDataStatus(data);
        if (rows == 0) {
            throw new ServiceException("填报状态已变化，请刷新后重试", CollectErrorCode.DATA_VERSION_CONFLICT);
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
        if (list.size() > exportMaxRecords()) {
            throw new ServiceException("导出数据超过单次上限（10 万条），请缩小筛选范围或改用异步导出");
        }

        // 先按 data_id 计数再决定加载哪些快照，避免把超额快照整体载入内存后才拒绝
        Map<Long, Long> cellCounts = countCellsByDataId(list);
        for (CollectData record : list) {
            Long count = cellCounts.get(record.getDataId());
            if (count == null || count == 0) {
                record.setExportNote("无填报内容");
            } else if (count > EXPORT_MAX_CELLS_PER_RECORD) {
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
                Long count = cellCounts.get(record.getDataId());
                if (count == null || count == 0 || count > EXPORT_MAX_CELLS_PER_RECORD) {
                    // 已在汇总表“备注”列说明，不生成工作表
                    continue;
                }
                // 逐记录加载快照：内存上界为单条记录（≤5 万格），即使批量记录也不会同时驻留
                List<CollectDataCell> cells = collectDataCellMapper.selectCollectDataCellByDataId(record.getDataId());
                if (cells.isEmpty()) {
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

    /** 单次导出记录数上限；独立成方法便于测试场景收窄 */
    protected int exportMaxRecords() {
        return EXPORT_MAX_RECORDS;
    }

    /** 按填报记录统计单元格快照数：data_id → 行数 */
    private Map<Long, Long> countCellsByDataId(List<CollectData> list) {
        Map<Long, Long> result = new HashMap<>();
        if (list.isEmpty()) {
            return result;
        }
        List<Long> dataIds = new ArrayList<>(list.size());
        for (CollectData record : list) {
            dataIds.add(record.getDataId());
        }
        for (Map<String, Object> row : collectDataCellMapper.countCollectDataCellByDataIds(dataIds)) {
            Object dataId = row.get("dataId");
            Object cellCount = row.get("cellCount");
            if (dataId instanceof Number id && cellCount instanceof Number count) {
                result.put(id.longValue(), count.longValue());
            }
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
            throw new ServiceException("已提交的数据不能再次修改或提交", CollectErrorCode.DATA_STATUS_NOT_EDITABLE);
        }
    }

    /**
     * 部门级数据权限（功能模块设计 §1.7）：按当前用户各角色的数据范围生成过滤 SQL，
     * 供 Mapper 以 ${params.dataScopeSql} 拼接（OR 语义，与 RuoYi @DataScope 一致）。
     * collect_data 无 user_id 列，“仅本人”用 create_by 表达；任一角色为“全部”时返回 null 表示不过滤。
     * 内容全部来自服务端角色元数据与转义后的当前用户名，非用户输入。
     */
    protected String buildDataScopeSql() {
        Long deptId = currentDeptId();
        String username = currentUsername().replace("'", "''");
        StringBuilder scope = new StringBuilder();
        List<SysRole> roles = currentUserRoles();
        if (roles != null) {
            for (SysRole role : roles) {
                if (role == null || role.getDataScope() == null) {
                    continue;
                }
                switch (role.getDataScope()) {
                    case "1":
                        // 全部数据权限
                        return null;
                    case "2":
                        if (role.getRoleId() != null) {
                            scope.append(" OR cd.dept_id IN (SELECT dept_id FROM sys_role_dept WHERE role_id = ")
                                    .append(role.getRoleId()).append(')');
                        }
                        break;
                    case "3":
                        if (deptId != null) {
                            scope.append(" OR cd.dept_id = ").append(deptId);
                        }
                        break;
                    case "4":
                        if (deptId != null) {
                            scope.append(" OR cd.dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_id = ")
                                    .append(deptId).append(" OR FIND_IN_SET(").append(deptId).append(", ancestors))");
                        }
                        break;
                    case "5":
                        scope.append(" OR cd.create_by = '").append(username).append('\'');
                        break;
                    default:
                        break;
                }
            }
        }
        if (scope.length() == 0) {
            // 无角色或角色均未配置数据范围时兜底为“仅本人”，避免默认越权全量可见
            scope.append(" OR cd.create_by = '").append(username).append('\'');
        }
        return "(" + scope.substring(4) + ")";
    }

    /**
     * 提交校验（功能模块设计 §1.5 前后端双重校验的后端兜底）：
     * 依据模板 dataVerification 中的标记校验必填（collectRequired）与字典/下拉合法性（collectDict 或 Luckysheet 原生 dropdown）。
     * 规则解析失败时降级跳过，不阻断提交（结构合法性已由 parseLuckysheetJson 保证）。
     */
    protected void validateCellRules(List<CollectDataCell> cells, String templateJson) {
        Map<String, CellRule> rules = extractCellRules(templateJson);
        if (rules.isEmpty()) {
            return;
        }
        Map<String, String> cellTexts = new HashMap<>();
        for (CollectDataCell cell : cells) {
            cellTexts.put(cell.getSheetIndex() + "!" + cell.getRowIndex() + "_" + cell.getColIndex(), cell.getCellText());
        }
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, CellRule> entry : rules.entrySet()) {
            CellRule rule = entry.getValue();
            String text = cellTexts.get(entry.getKey());
            String trimmed = text == null ? "" : text.trim();
            if (rule.required && trimmed.isEmpty()) {
                errors.add(rule.location + "为必填项");
                continue;
            }
            if (trimmed.isEmpty() || rule.allowedValues == null) {
                continue;
            }
            if (!rule.allowedValues.contains(trimmed)) {
                errors.add(rule.location + "的值“" + StringUtils.abbreviate(trimmed, 20) + "”不在可选范围内");
            }
        }
        if (!errors.isEmpty()) {
            throw new ServiceException("提交校验未通过：" + String.join("；", errors));
        }
    }

    /**
     * 从模板工作簿 JSON 提取单元格验证规则：key 为 "sheetIndex!row,col"。
     * collectDict 指向 RuoYi 字典类型（合法值 = 字典标签 + 键值），value1 为 Luckysheet 原生下拉选项。
     */
    @SuppressWarnings("unchecked")
    protected Map<String, CellRule> extractCellRules(String templateJson) {
        Map<String, CellRule> rules = new HashMap<>();
        if (templateJson == null || templateJson.isEmpty()) {
            return rules;
        }
        try {
            List<Map<String, Object>> root = MAPPER.readValue(templateJson, List.class);
            if (root.isEmpty() || !(root.get(0) instanceof Map) || root.get(0).containsKey("r") || root.get(0).containsKey("c")) {
                // 平铺单元格格式不携带 dataVerification，直接跳过
                return rules;
            }
            for (int sheetIndex = 0; sheetIndex < root.size(); sheetIndex++) {
                Map<String, Object> sheet = root.get(sheetIndex);
                if (!(sheet.get("dataVerification") instanceof Map)) {
                    continue;
                }
                String sheetName = sheet.get("name") instanceof String s && !s.isBlank() ? s : ("工作表" + (sheetIndex + 1));
                Map<String, Object> verifications = (Map<String, Object>) sheet.get("dataVerification");
                for (Map.Entry<String, Object> entry : verifications.entrySet()) {
                    if (!(entry.getValue() instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> conf = (Map<String, Object>) entry.getValue();
                    boolean required = Boolean.TRUE.equals(conf.get("collectRequired"));
                    String dictType = conf.get("collectDict") instanceof String s && !s.isBlank() ? s : null;
                    String type = conf.get("type") instanceof String s ? s : null;
                    if (!required && dictType == null && !"dropdown".equals(type)) {
                        continue;
                    }
                    // Luckysheet dataVerification 键格式为 "r_c"（见 fork dataVerificationCtrl）
                    String[] rc = entry.getKey().split("_");
                    if (rc.length != 2) {
                        continue;
                    }
                    CellRule rule = new CellRule();
                    rule.required = required;
                    rule.location = sheetName + " " + toCellRef(Integer.parseInt(rc[0].trim()), Integer.parseInt(rc[1].trim()));
                    Set<String> allowed = new LinkedHashSet<>();
                    if (dictType != null) {
                        appendDictValues(allowed, dictType);
                    }
                    if ("dropdown".equals(type) && conf.get("value1") instanceof String v1) {
                        for (String option : v1.split(",")) {
                            if (!option.isBlank()) {
                                allowed.add(option.trim());
                            }
                        }
                    }
                    rule.allowedValues = allowed.isEmpty() ? null : allowed;
                    rules.put(sheetIndex + "!" + entry.getKey(), rule);
                }
            }
        } catch (Exception e) {
            log.warn("解析模板数据验证规则失败，本次提交跳过字段校验: {}", e.getMessage());
        }
        return rules;
    }

    /** 0-based 行列号转 Excel 风格引用（如 B3），用于校验错误提示定位 */
    private String toCellRef(int row, int col) {
        StringBuilder colRef = new StringBuilder();
        int c = col;
        do {
            colRef.insert(0, (char) ('A' + c % 26));
            c = c / 26 - 1;
        } while (c >= 0);
        return colRef + String.valueOf(row + 1);
    }

    /** 字典合法值 = 标签 + 键值；字典缓存不可用时返回空集（该单元格仅保留必填校验） */
    protected void appendDictValues(Set<String> allowed, String dictType) {
        try {
            List<SysDictData> dictData = DictUtils.getDictCache(dictType);
            if (dictData == null) {
                return;
            }
            for (SysDictData d : dictData) {
                if (StringUtils.isNotBlank(d.getDictLabel())) {
                    allowed.add(d.getDictLabel().trim());
                }
                if (StringUtils.isNotBlank(d.getDictValue())) {
                    allowed.add(d.getDictValue().trim());
                }
            }
        } catch (Exception e) {
            log.warn("读取字典 {} 缓存失败，跳过取值校验: {}", dictType, e.getMessage());
        }
    }

    protected List<SysRole> currentUserRoles() {
        return SecurityUtils.getLoginUser().getUser().getRoles();
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

    /** 模板单元格验证规则（提交校验用） */
    protected static final class CellRule {
        boolean required;
        String location;
        /** 合法值集合；null 表示不校验取值（仅必填） */
        Set<String> allowedValues;
    }
}
