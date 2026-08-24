package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectFieldMapping;
import com.allinone.collect.mapper.CollectDataMapper;
import com.allinone.collect.mapper.CollectFieldMappingMapper;
import com.allinone.collect.service.IDataWriteBackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据回写引擎（三层架构 Tier 3）
 * 将 collect_data.form_data（JSON）按 collect_field_mapping 配置
 * 回写到目标业务表（biz_xxx）
 */
@Service
public class DataWriteBackServiceImpl implements IDataWriteBackService {

    private static final Logger log = LoggerFactory.getLogger(DataWriteBackServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 表名校验正则：仅允许字母、数字、下划线 */
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    @Autowired
    private CollectDataMapper collectDataMapper;

    @Autowired
    private CollectFieldMappingMapper collectFieldMappingMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void writeBack(Long dataId) {
        CollectData data = collectDataMapper.selectCollectDataById(dataId);
        if (data == null) return;
        writeBack(data);
    }

    @Override
    @Transactional
    public void writeBack(CollectData data) {
        List<CollectFieldMapping> mappings = collectFieldMappingMapper
                .selectCollectFieldMappingByTemplate(data.getTemplateId());
        if (mappings == null || mappings.isEmpty()) return;

        // 按目标表分组
        Map<String, List<CollectFieldMapping>> tableGroups = mappings.stream()
                .collect(Collectors.groupingBy(CollectFieldMapping::getTargetTable));

        // 解析 form_data JSON 获取单元格值映射：(row,col) → cellValue
        Map<String, String> cellValueMap = parseFormDataToCellMap(data.getFormData());

        for (Map.Entry<String, List<CollectFieldMapping>> entry : tableGroups.entrySet()) {
            String tableName = entry.getKey();
            List<CollectFieldMapping> fields = entry.getValue();
            executeWriteBack(tableName, fields, cellValueMap);
        }
    }

    private void executeWriteBack(String tableName, List<CollectFieldMapping> fields,
                                  Map<String, String> cellValueMap) {
        // 表名白名单校验，防止 SQL 注入
        if (tableName == null || !TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            log.warn("非法表名，跳过回写: tableName={}", tableName);
            return;
        }

        List<CollectFieldMapping> pkFields = fields.stream()
                .filter(f -> f.getPkOrder() != null && f.getPkOrder() > 0)
                .sorted(Comparator.comparing(CollectFieldMapping::getPkOrder))
                .collect(Collectors.toList());
        List<CollectFieldMapping> valueFields = fields.stream()
                .filter(f -> f.getPkOrder() == null || f.getPkOrder() == 0)
                .collect(Collectors.toList());

        if (pkFields.isEmpty() || valueFields.isEmpty()) return;

        // 构建 UPSERT: INSERT ... ON DUPLICATE KEY UPDATE
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");

        List<String> allCols = new ArrayList<>();
        List<String> allVals = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        for (CollectFieldMapping f : fields) {
            // 列名也做校验
            String col = f.getTargetColumn();
            if (col == null || !TABLE_NAME_PATTERN.matcher(col).matches()) {
                log.warn("非法列名，跳过: table={}, col={}", tableName, col);
                return;
            }
            allCols.add(col);
            String key = f.getRowIndex() + "," + f.getColIndex();
            String val = cellValueMap.getOrDefault(key, f.getDefaultValue());
            allVals.add("?");
            params.add(val);
        }

        sql.append(String.join(", ", allCols));
        sql.append(") VALUES (");
        sql.append(String.join(", ", allVals));
        sql.append(") ON DUPLICATE KEY UPDATE ");

        List<String> updates = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            CollectFieldMapping f = fields.get(i);
            if (f.getPkOrder() == null || f.getPkOrder() == 0) {
                updates.add(f.getTargetColumn() + " = ?");
                String key = f.getRowIndex() + "," + f.getColIndex();
                String val = cellValueMap.getOrDefault(key, f.getDefaultValue());
                params.add(val);
            }
        }
        sql.append(String.join(", ", updates));

        log.debug("执行UPSERT: table={}, fields={}", tableName, fields.size());
        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    /**
     * 解析 Luckysheet JSON 为 (row,col) → cell_text 映射
     * JSON 格式: [{ "r":0, "c":0, "v":{ "v":"值", "m":"显示文本" } }, ...]
     */
    private Map<String, String> parseFormDataToCellMap(String formData) {
        Map<String, String> map = new HashMap<>();
        if (formData == null || formData.isEmpty()) return map;

        try {
            List<Map<String, Object>> cells = MAPPER.readValue(formData, List.class);

            for (Map<String, Object> cell : cells) {
                int r = ((Number) cell.getOrDefault("r", 0)).intValue();
                int c = ((Number) cell.getOrDefault("c", 0)).intValue();
                String key = r + "," + c;

                Object vObj = cell.get("v");
                if (vObj instanceof Map) {
                    Object mVal = ((Map) vObj).get("m");
                    Object vVal = ((Map) vObj).get("v");
                    map.put(key, mVal != null ? mVal.toString() :
                            (vVal != null ? vVal.toString() : ""));
                }
            }
        } catch (Exception e) {
            log.warn("Luckysheet JSON解析失败 formData长度={}", formData != null ? formData.length() : 0, e);
        }
        return map;
    }
}
