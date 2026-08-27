package com.allinone.collect.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import com.allinone.common.annotation.Log;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.common.utils.DateUtils;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.poi.ExcelUtil;
import com.allinone.common.utils.uuid.IdUtils;
import com.allinone.collect.domain.*;
import com.allinone.collect.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/collect/report")
public class WorkReportController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(WorkReportController.class);
    @Autowired private IWorkReportService workReportService;
    @Autowired private IWorkReportSheetService workReportSheetService;
    @Autowired private IWorkReportCellService workReportCellService;
    @Autowired private IWorkReportSheetPermissionService permissionService;
    @Autowired private WorkReportAccessService workReportAccessService;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_SHEETS_PER_REPORT = 100;
    private static final int MAX_CELLS_PER_REQUEST = 5000;
    private static final int MAX_RANGE_CELLS = 10000;
    private static final int CELL_BATCH_SIZE = 500;

    // ==================== Report CRUD ====================
    @PreAuthorize("@ss.hasPermi('collect:report:list')")
    @GetMapping("/list")
    public TableDataInfo list(WorkReport workReport) {
        startPage();
        List<WorkReport> list = workReportService.selectWorkReportList(workReport);
        return getDataTable(list);
    }
    @PreAuthorize("@ss.hasPermi('collect:report:export')")
    @Log(title = "报表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WorkReport workReport) {
        List<WorkReport> list = workReportService.selectWorkReportList(workReport);
        ExcelUtil<WorkReport> util = new ExcelUtil<>(WorkReport.class);
        util.exportExcel(response, list, "报表数据");
    }
    @PreAuthorize("@ss.hasPermi('collect:report:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable String id) {
        return success(workReportService.selectWorkReportById(id));
    }
    @PreAuthorize("@ss.hasPermi('collect:report:add')")
    @Log(title = "报表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WorkReport workReport) {
        return toAjax(workReportService.insertWorkReport(workReport));
    }
    @PreAuthorize("@ss.hasPermi('collect:report:edit')")
    @Log(title = "报表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WorkReport workReport) {
        return toAjax(workReportService.updateWorkReport(workReport));
    }
    @PreAuthorize("@ss.hasPermi('collect:report:remove')")
    @Log(title = "报表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(workReportService.deleteWorkReportByIds(ids));
    }

    // ==================== Sheet 保存/加载 ====================
    @SuppressWarnings("unchecked")
    @PreAuthorize("@ss.hasPermi('collect:report:edit')")
    @Log(title = "报表Sheet", businessType = BusinessType.UPDATE)
    @PutMapping("/sheet/{reportId}")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult saveSheet(@PathVariable String reportId, @RequestBody Map<String, Object> body) {
        // 使用访问服务校验编辑权限，只有属主或管理员可以编辑
        WorkReport report = workReportAccessService.requireReportOwnerOrAdmin(reportId);
        Object data = body.get("data");
        if (data == null) return error("表格数据不能为空");
        if (!(data instanceof List<?> rawSheets)) return error("表格数据格式错误");
        List<Map<String, Object>> sheetList = new ArrayList<>();
        for (Object rawSheet : rawSheets) {
            if (!(rawSheet instanceof Map<?, ?>)) return error("Sheet数据格式错误");
            sheetList.add((Map<String, Object>) rawSheet);
        }
        if (sheetList.size() > MAX_SHEETS_PER_REPORT) return error("单个报表最多包含100个Sheet");
        WorkReportSheet query = new WorkReportSheet();
        query.setReportId(reportId);
        List<WorkReportSheet> existingSheets = workReportSheetService.selectAccessibleSheets(query);
        Map<String, WorkReportSheet> existingMap = existingSheets.stream()
            .collect(Collectors.toMap(WorkReportSheet::getId, s -> s));
        Set<String> deletedSheetIds = new HashSet<>();
        Object rawDeletedSheetIds = body.get("deletedSheetIds");
        if (rawDeletedSheetIds instanceof List<?> rawDeletedList) {
            for (Object rawId : rawDeletedList) {
                if (!(rawId instanceof String id) || id.isBlank()) return error("待删除Sheet ID格式错误");
                if (!existingMap.containsKey(id)) return error("待删除Sheet不存在或无权访问");
                deletedSheetIds.add(id);
            }
        } else if (rawDeletedSheetIds != null) {
            return error("待删除Sheet列表格式错误");
        }
        Set<String> referencedSheetIds = new HashSet<>();
        // 预解析全部 Sheet 的顺序号与名称：必须在写入循环前完成校验，
        // 否则中途 return error 会绕过 @Transactional 回滚造成部分提交
        List<Integer> sheetOrderList = new ArrayList<>();
        for (Map<String, Object> sheetObj : sheetList) {
            Integer sheetIndex = asInt(sheetObj.get("order"), asInt(sheetObj.get("index"), 0));
            if (sheetIndex == null) return error("Sheet顺序(order/index)格式错误");
            sheetOrderList.add(sheetIndex);
            if (isNonString(sheetObj.get("name"))) return error("Sheet数据格式错误");
            Object rawSheetDbId = sheetObj.get("_sheetDbId");
            if (rawSheetDbId == null) continue;
            if (!(rawSheetDbId instanceof String sheetDbId) || sheetDbId.isBlank()) {
                return error("Sheet ID格式错误");
            }
            if (!referencedSheetIds.add(sheetDbId)) return error("Sheet ID不能重复");
            if (deletedSheetIds.contains(sheetDbId)) return error("同一Sheet不能同时保存和删除");
        }
        List<Map<String, String>> idMappings = new ArrayList<>();
        for (int i = 0; i < sheetList.size(); i++) {
            Map<String, Object> sheetObj = sheetList.get(i);
            String sheetName = asString(sheetObj.get("name"));
            Integer sheetIndex = sheetOrderList.get(i);
            String sheetDbId = asString(sheetObj.get("_sheetDbId"));
            String clientSheetId = String.valueOf(sheetObj.getOrDefault("index", sheetIndex));
            Map<String, Object> metaOnly = extractMeta(sheetObj, sheetName);
            if (sheetDbId != null && existingMap.containsKey(sheetDbId)) {
                WorkReportSheet us = new WorkReportSheet();
                us.setId(sheetDbId); us.setSheetName(sheetName); us.setSheetIndex(sheetIndex.longValue());
                try { us.setSheetData(MAPPER.writeValueAsString(metaOnly)); } catch (Exception e) {
                    log.warn("Sheet元数据序列化失败 reportId={}", reportId, e);
                }
                us.setUpdateTime(DateUtils.getNowDate());
                workReportSheetService.updateWorkReportSheet(us);
            } else {
                sheetDbId = IdUtils.fastUUID();
                // 使用访问服务创建Sheet，确保属主正确
                WorkReportSheet ns = workReportAccessService.createSheetWithCorrectOwner(reportId, sheetName, sheetIndex.longValue());
                ns.setId(sheetDbId);
                try { ns.setSheetData(MAPPER.writeValueAsString(metaOnly)); } catch (Exception e) {
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
        for (String deletedSheetId : deletedSheetIds) {
            workReportCellService.deleteCellsBySheetId(deletedSheetId);
            workReportSheetService.deleteWorkReportSheetById(deletedSheetId);
        }
        return success(idMappings);
    }
    @PreAuthorize("@ss.hasPermi('collect:report:query')")
    @GetMapping("/sheet/{reportId}")
    public AjaxResult getSheet(@PathVariable String reportId) {
        WorkReport report = workReportService.selectWorkReportById(reportId);
        if (report == null) return error("报表不存在");
        WorkReportSheet query = new WorkReportSheet();
        query.setReportId(reportId);
        List<WorkReportSheet> sheets = workReportSheetService.selectAccessibleSheets(query);
        if (sheets.isEmpty()) {
            ArrayNode result = MAPPER.createArrayNode();
            result.add(createDefaultSheetJson("Sheet1", 0));
            return success(result);
        }
        ArrayNode result = MAPPER.createArrayNode();
        for (WorkReportSheet sheet : sheets) result.add(buildLightweightMeta(sheet));
        return success(result);
    }
    // ==================== Cell 范围加载/保存 ====================
    @PreAuthorize("@ss.hasPermi('collect:report:query')")
    @GetMapping("/cells")
    public AjaxResult loadCells(@RequestParam String sheetDbId,
        @RequestParam(defaultValue = "0") int startRow, @RequestParam(defaultValue = "99") int endRow,
        @RequestParam(defaultValue = "0") int startCol, @RequestParam(defaultValue = "29") int endCol) {
        WorkReportSheet sheet = workReportSheetService.selectAccessibleSheetById(sheetDbId);
        if (sheet == null) return error("Sheet不存在或无权访问");
        if (startRow < 0 || startCol < 0 || endRow < startRow || endCol < startCol)
            return error("单元格范围参数无效");
        long requestedCells = (long) (endRow - startRow + 1) * (endCol - startCol + 1);
        if (requestedCells > MAX_RANGE_CELLS) return error("单次最多加载10000个单元格");
        return success(workReportCellService.selectCellsByRange(sheetDbId, startRow, endRow, startCol, endCol));
    }
    @PreAuthorize("@ss.hasPermi('collect:report:edit')")
    @Log(title = "报表单元格", businessType = BusinessType.UPDATE)
    @PutMapping("/cells")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult saveCells(@RequestBody Map<String, Object> body) {
        Object raw = body.get("cells");
        if (raw == null) return success("无变更");
        if (!(raw instanceof List<?> rawCells)) return error("单元格数据格式错误");
        if (rawCells.isEmpty()) return success("无变更");
        if (rawCells.size() > MAX_CELLS_PER_REQUEST) return error("单次最多保存5000个单元格");
        List<WorkReportCell> cells = new ArrayList<>(rawCells.size());
        Set<String> sheetIds = new HashSet<>();
        for (Object cellObj : rawCells) {
            if (!(cellObj instanceof Map<?, ?> cell)) return error("单元格数据格式错误");
            Object rawSheetDbId = cell.get("sheetDbId");
            if (rawSheetDbId == null) return error("缺少Sheet ID");
            String sheetDbId = asString(rawSheetDbId);
            if (sheetDbId == null || sheetDbId.isBlank()) return error("单元格数据格式错误");
            Integer rowIndex = asInt(cell.get("rowIndex"), 0);
            Integer colIndex = asInt(cell.get("colIndex"), 0);
            if (rowIndex == null || colIndex == null) return error("单元格数据格式错误");
            if (isNonString(cell.get("cellValue")) || isNonString(cell.get("cellFormula"))
                || isNonString(cell.get("cellType"))) {
                return error("单元格数据格式错误");
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
        // 使用访问服务校验每个Sheet的编辑权限
        for (String sheetId : sheetIds) {
            workReportAccessService.requireEditableSheet(sheetId);
        }
        if (cells.stream().anyMatch(c -> c.getRowIndex() < 0 || c.getColIndex() < 0))
            return error("单元格坐标不能为负数");
        for (int from = 0; from < cells.size(); from += CELL_BATCH_SIZE) {
            workReportCellService.batchUpsertCells(cells.subList(from, Math.min(from + CELL_BATCH_SIZE, cells.size())));
        }
        return success("保存成功");
    }

    // ==================== 显式权限分配 ====================
    @PreAuthorize("@ss.hasPermi('collect:report:query')")
    @GetMapping("/permissions/{sheetDbId}")
    public AjaxResult listPermissions(@PathVariable String sheetDbId) {
        if (workReportSheetService.selectAccessibleSheetById(sheetDbId) == null) return error("Sheet不存在或无权访问");
        return success(permissionService.listBySheet(sheetDbId));
    }
    @PreAuthorize("@ss.hasPermi('collect:report:edit')")
    @Log(title = "权限", businessType = BusinessType.GRANT)
    @PostMapping("/permissions/{sheetDbId}")
    public AjaxResult grantPermission(@PathVariable String sheetDbId, @RequestBody Map<String, Object> body) {
        // 使用访问服务校验权限管理权限
        workReportAccessService.requireSheetOwnerOrAdmin(sheetDbId);
        String permType = (String) body.get("permType");
        Number permIdNum = (Number) body.get("permId");
        if (permType == null || permIdNum == null) return error("缺少permType或permId");
        if (!Set.of("role", "dept", "user").contains(permType) || permIdNum.longValue() <= 0)
            return error("权限类型或目标ID无效");
        WorkReportSheetPermission p = new WorkReportSheetPermission();
        p.setSheetId(sheetDbId); p.setPermType(permType);
        p.setPermId(permIdNum.longValue()); p.setGrantedBy(SecurityUtils.getUserId());
        permissionService.grant(p);
        return success("权限分配成功");
    }
    @PreAuthorize("@ss.hasPermi('collect:report:edit')")
    @Log(title = "权限", businessType = BusinessType.DELETE)
    @DeleteMapping("/permissions/{sheetDbId}")
    public AjaxResult revokePermission(@PathVariable String sheetDbId,
        @RequestParam String permType, @RequestParam Long permId) {
        // 使用访问服务校验权限管理权限
        workReportAccessService.requireSheetOwnerOrAdmin(sheetDbId);
        if (!Set.of("role", "dept", "user").contains(permType) || permId <= 0)
            return error("权限类型或目标ID无效");
        permissionService.revoke(sheetDbId, permType, permId);
        return success("权限撤销成功");
    }

    // ==================== 内部工具 ====================
    /** 仅接受 String 类型（null 亦返回 null），避免对客户端报文直接强转抛 ClassCastException */
    private static String asString(Object value) {
        return value instanceof String s ? s : null;
    }

    /**
     * 宽松整数解析：null 返回默认值，兼容 Number 与数字字符串（如 "3"），
     * 无法解析返回 null 表示报文非法
     */
    private static Integer asInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /** 报文合法性检查：字段存在但不是字符串 */
    private static boolean isNonString(Object value) {
        return value != null && !(value instanceof String);
    }

    private Map<String, Object> extractMeta(Map<String, Object> src, String name) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        String[] keys = {"color","status","order","index","row","column","config",
            "columnWidth","rowHeight","merges","images","chart","dataVerification","filter"};
        for (String k : keys) if (src.containsKey(k)) m.put(k, src.get(k));
        return m;
    }
    private ObjectNode buildLightweightMeta(WorkReportSheet sheet) {
        ObjectNode m = MAPPER.createObjectNode();
        m.put("_sheetDbId", sheet.getId());
        m.put("name", sheet.getSheetName());
        int idx = sheet.getSheetIndex() != null ? sheet.getSheetIndex().intValue() : 0;
        m.put("index", idx); m.put("order", idx);
        m.put("status", idx == 0 ? "1" : "0");
        m.putArray("celldata");
        if (sheet.getSheetData() != null && !sheet.getSheetData().isEmpty()) {
            try {
                ObjectNode stored = (ObjectNode) MAPPER.readTree(sheet.getSheetData());
                String[] keys = {"config","columnWidth","rowHeight","merges","images","chart","dataVerification","filter","row","column"};
                for (String k : keys) if (stored.has(k)) m.set(k, stored.get(k));
            } catch (Exception e) {
                log.warn("Sheet JSON解析失败 sheetId={}", sheet.getId(), e);
            }
        }
        return m;
    }
    private ObjectNode createDefaultSheetJson(String name, int index) {
        ObjectNode s = MAPPER.createObjectNode();
        s.put("name", name); s.put("color", "");
        s.put("status", index == 0 ? "1" : "0");
        s.put("order", index); s.put("index", index);
        s.set("config", MAPPER.createObjectNode());
        return s;
    }
}
