package com.allinone.collect.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.collect.domain.WorkReport;
import com.allinone.collect.domain.WorkReportCell;
import com.allinone.collect.domain.WorkReportSheet;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 报表 Sheet 元数据与单元格快照的写入服务。
 * 事务边界收口在 Service 层（Controller 只做鉴权与参数转发），
 * 与 RuoYi 的 Controller → Service → Mapper 分层约定保持一致。
 */
@Service
public class WorkReportSheetWriteService
{
    private static final Logger log = LoggerFactory.getLogger(WorkReportSheetWriteService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_SHEETS_PER_REPORT = 100;
    private static final int MAX_CELLS_PER_REQUEST = 5000;
    private static final int CELL_BATCH_SIZE = 500;

    @Autowired
    private IWorkReportSheetService workReportSheetService;

    @Autowired
    private IWorkReportCellService workReportCellService;

    @Autowired
    private WorkReportAccessService workReportAccessService;

    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, String>> saveSheet(String reportId, Map<String, Object> body)
    {
        // 使用访问服务校验编辑权限，只有属主或管理员可以编辑
        WorkReport report = workReportAccessService.requireReportOwnerOrAdmin(reportId);
        Object data = body.get("data");
        if (data == null) throw new ServiceException("表格数据不能为空");
        if (!(data instanceof List<?> rawSheets)) throw new ServiceException("表格数据格式错误");
        List<Map<String, Object>> sheetList = new ArrayList<>();
        for (Object rawSheet : rawSheets)
        {
            if (!(rawSheet instanceof Map<?, ?>)) throw new ServiceException("Sheet数据格式错误");
            sheetList.add((Map<String, Object>) rawSheet);
        }
        if (sheetList.size() > MAX_SHEETS_PER_REPORT) throw new ServiceException("单个报表最多包含100个Sheet");
        WorkReportSheet query = new WorkReportSheet();
        query.setReportId(reportId);
        List<WorkReportSheet> existingSheets = workReportSheetService.selectAccessibleSheets(query);
        Map<String, WorkReportSheet> existingMap = existingSheets.stream()
            .collect(Collectors.toMap(WorkReportSheet::getId, s -> s));
        Set<String> deletedSheetIds = new HashSet<>();
        Object rawDeletedSheetIds = body.get("deletedSheetIds");
        if (rawDeletedSheetIds instanceof List<?> rawDeletedList)
        {
            for (Object rawId : rawDeletedList)
            {
                if (!(rawId instanceof String id) || id.isBlank()) throw new ServiceException("待删除Sheet ID格式错误");
                if (!existingMap.containsKey(id)) throw new ServiceException("待删除Sheet不存在或无权访问");
                deletedSheetIds.add(id);
            }
        }
        else if (rawDeletedSheetIds != null)
        {
            throw new ServiceException("待删除Sheet列表格式错误");
        }
        Set<String> referencedSheetIds = new HashSet<>();
        // 预解析全部 Sheet 的顺序号与名称：必须在写入循环前完成校验，
        // 否则中途抛出异常虽会回滚，但白耗一轮连接持有时间
        List<Integer> sheetOrderList = new ArrayList<>();
        for (Map<String, Object> sheetObj : sheetList)
        {
            Integer sheetIndex = asInt(sheetObj.get("order"), asInt(sheetObj.get("index"), 0));
            if (sheetIndex == null) throw new ServiceException("Sheet顺序(order/index)格式错误");
            sheetOrderList.add(sheetIndex);
            if (isNonString(sheetObj.get("name"))) throw new ServiceException("Sheet数据格式错误");
            Object rawSheetDbId = sheetObj.get("_sheetDbId");
            if (rawSheetDbId == null) continue;
            if (!(rawSheetDbId instanceof String sheetDbId) || sheetDbId.isBlank())
            {
                throw new ServiceException("Sheet ID格式错误");
            }
            if (!referencedSheetIds.add(sheetDbId)) throw new ServiceException("Sheet ID不能重复");
            if (deletedSheetIds.contains(sheetDbId)) throw new ServiceException("同一Sheet不能同时保存和删除");
        }
        List<Map<String, String>> idMappings = new ArrayList<>();
        for (int i = 0; i < sheetList.size(); i++)
        {
            Map<String, Object> sheetObj = sheetList.get(i);
            String sheetName = asString(sheetObj.get("name"));
            Integer sheetIndex = sheetOrderList.get(i);
            String sheetDbId = asString(sheetObj.get("_sheetDbId"));
            String clientSheetId = String.valueOf(sheetObj.getOrDefault("index", sheetIndex));
            Map<String, Object> metaOnly = extractMeta(sheetObj, sheetName);
            if (sheetDbId != null && existingMap.containsKey(sheetDbId))
            {
                WorkReportSheet us = new WorkReportSheet();
                us.setId(sheetDbId); us.setSheetName(sheetName); us.setSheetIndex(sheetIndex.longValue());
                try { us.setSheetData(MAPPER.writeValueAsString(metaOnly)); } catch (Exception e)
                {
                    log.warn("Sheet元数据序列化失败 reportId={}", reportId, e);
                }
                us.setUpdateTime(DateUtils.getNowDate());
                workReportSheetService.updateWorkReportSheet(us);
            }
            else
            {
                sheetDbId = IdUtils.fastUUID();
                // 使用访问服务创建Sheet，确保属主正确
                WorkReportSheet ns = workReportAccessService.createSheetWithCorrectOwner(reportId, sheetName, sheetIndex.longValue());
                ns.setId(sheetDbId);
                try { ns.setSheetData(MAPPER.writeValueAsString(metaOnly)); } catch (Exception e)
                {
                    log.warn("Sheet元数据序列化失败 reportId={}", reportId, e);
                }
                ns.setCreateTime(DateUtils.getNowDate());
                workReportSheetService.insertWorkReportSheet(ns);
            }
            Map<String, String> mapping = new HashMap<>();
            mapping.put("clientSheetId", clientSheetId);
            mapping.put("sheetDbId", sheetDbId);
            idMappings.add(mapping);
        }
        for (String deletedSheetId : deletedSheetIds)
        {
            workReportCellService.deleteCellsBySheetId(deletedSheetId);
            workReportSheetService.deleteWorkReportSheetById(deletedSheetId);
        }
        return idMappings;
    }

    /**
     * 保存单元格快照（last-write-win + 乐观锁）。
     * 客户端可携带 sheetVersions（{sheetDbId: 期望版本号}）：
     * 版本与库中一致时递增并写入，不一致说明他人已保存过，抛异常回滚防止静默覆盖；
     * 未携带版本时保持旧行为，兼容升级窗口内的旧前端。
     *
     * @return 各 Sheet 递增后的最新版本号（仅对携带了期望版本的 Sheet 返回）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Long> saveCells(Map<String, Object> body)
    {
        Object raw = body.get("cells");
        if (raw == null) return new HashMap<>();
        if (!(raw instanceof List<?> rawCells)) throw new ServiceException("单元格数据格式错误");
        if (rawCells.isEmpty()) return new HashMap<>();
        if (rawCells.size() > MAX_CELLS_PER_REQUEST) throw new ServiceException("单次最多保存5000个单元格");
        List<WorkReportCell> cells = new ArrayList<>(rawCells.size());
        Set<String> sheetIds = new HashSet<>();
        for (Object cellObj : rawCells)
        {
            if (!(cellObj instanceof Map<?, ?> cell)) throw new ServiceException("单元格数据格式错误");
            Object rawSheetDbId = cell.get("sheetDbId");
            if (rawSheetDbId == null) throw new ServiceException("缺少Sheet ID");
            String sheetDbId = asString(rawSheetDbId);
            if (sheetDbId == null || sheetDbId.isBlank()) throw new ServiceException("单元格数据格式错误");
            Integer rowIndex = asInt(cell.get("rowIndex"), 0);
            Integer colIndex = asInt(cell.get("colIndex"), 0);
            if (rowIndex == null || colIndex == null) throw new ServiceException("单元格数据格式错误");
            if (isNonString(cell.get("cellValue")) || isNonString(cell.get("cellFormula"))
                || isNonString(cell.get("cellType")))
            {
                throw new ServiceException("单元格数据格式错误");
            }
            WorkReportCell c = new WorkReportCell();
            c.setSheetId(sheetDbId);
            c.setRowIndex(rowIndex);
            c.setColIndex(colIndex);
            c.setCellValue(asString(cell.get("cellValue")));
            c.setCellFormula(asString(cell.get("cellFormula")));
            c.setCellType(asString(cell.get("cellType")));
            cells.add(c);
            sheetIds.add(sheetDbId);
        }
        // 逐 Sheet 校验编辑权限并做版本 CAS；任一 Sheet 版本过期即整体回滚
        Map<String, Number> expectedVersions = parseSheetVersions(body.get("sheetVersions"));
        Map<String, Long> newVersions = new HashMap<>();
        Date now = DateUtils.getNowDate();
        for (String sheetId : sheetIds)
        {
            workReportAccessService.requireEditableSheet(sheetId);
            Number expected = expectedVersions.get(sheetId);
            if (expected != null)
            {
                int rows = workReportSheetService.compareAndIncrementVersion(sheetId, expected.longValue(), now);
                if (rows == 0)
                {
                    throw new ServiceException("该 Sheet 已被他人修改，请刷新后重试");
                }
                // CAS 成功意味着版本恰好从 expected 迁移到 expected+1，无需回查
                newVersions.put(sheetId, expected.longValue() + 1);
            }
        }
        if (cells.stream().anyMatch(c -> c.getRowIndex() < 0 || c.getColIndex() < 0))
            throw new ServiceException("单元格坐标不能为负数");
        for (int from = 0; from < cells.size(); from += CELL_BATCH_SIZE)
        {
            workReportCellService.batchUpsertCells(cells.subList(from, Math.min(from + CELL_BATCH_SIZE, cells.size())));
        }
        return newVersions;
    }

    /** 宽松解析客户端携带的 sheetVersions：{sheetDbId: 期望版本号}，非法条目忽略（视为未携带，兼容旧前端） */
    private static Map<String, Number> parseSheetVersions(Object raw)
    {
        Map<String, Number> versions = new HashMap<>();
        if (!(raw instanceof Map<?, ?> rawMap))
        {
            return versions;
        }
        for (Map.Entry<?, ?> entry : rawMap.entrySet())
        {
            if (entry.getKey() instanceof String key && !key.isBlank() && entry.getValue() instanceof Number number)
            {
                versions.put(key, number);
            }
        }
        return versions;
    }

    private static Map<String, Object> extractMeta(Map<String, Object> src, String name)
    {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        String[] keys = {"color","status","order","index","row","column","config",
            "columnWidth","rowHeight","merges","images","chart","dataVerification","filter"};
        for (String k : keys) if (src.containsKey(k)) m.put(k, src.get(k));
        return m;
    }

    /** 仅接受 String 类型（null 亦返回 null），避免对客户端报文直接强转抛 ClassCastException */
    private static String asString(Object value)
    {
        return value instanceof String s ? s : null;
    }

    /**
     * 宽松整数解析：null 返回默认值，兼容 Number 与数字字符串（如 "3"），
     * 无法解析返回 null 表示报文非法
     */
    private static Integer asInt(Object value, int defaultValue)
    {
        if (value == null) return defaultValue;
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String str)
        {
            try
            {
                return Integer.parseInt(str.trim());
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }
        return null;
    }

    /** 报文合法性检查：字段存在但不是字符串 */
    private static boolean isNonString(Object value)
    {
        return value != null && !(value instanceof String);
    }
}
