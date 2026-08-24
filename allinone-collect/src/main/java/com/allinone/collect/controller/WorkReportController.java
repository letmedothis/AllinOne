package com.allinone.collect.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    public AjaxResult saveSheet(@PathVariable String reportId, @RequestBody Map<String, Object> body) {
        WorkReport report = workReportService.selectWorkReportById(reportId);
        if (report == null) return error("报表不存在");
        Object data = body.get("data");
        if (data == null) return error("表格数据不能为空");
        List<Map<String, Object>> sheetList = (List<Map<String, Object>>) data;
        WorkReportSheet query = new WorkReportSheet();
        query.setReportId(reportId);
        List<WorkReportSheet> existingSheets = workReportSheetService.selectAccessibleSheets(query);
        Map<String, WorkReportSheet> existingMap = existingSheets.stream()
            .collect(Collectors.toMap(WorkReportSheet::getId, s -> s));
        for (Map<String, Object> sheetObj : sheetList) {
            String sheetName = (String) sheetObj.get("name");
            Integer sheetIndex = sheetObj.get("order") != null ? Integer.parseInt(sheetObj.get("order").toString())
                : (sheetObj.get("index") != null ? Integer.parseInt(sheetObj.get("index").toString()) : 0);
            String sheetDbId = (String) sheetObj.get("_sheetDbId");
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
                WorkReportSheet ns = new WorkReportSheet();
                ns.setId(sheetDbId); ns.setReportId(reportId); ns.setSheetName(sheetName);
                ns.setSheetIndex(sheetIndex.longValue());
                try { ns.setSheetData(MAPPER.writeValueAsString(metaOnly)); } catch (Exception e) {
                    log.warn("Sheet元数据序列化失败 reportId={}", reportId, e);
                }
                ns.setUserId(SecurityUtils.getUserId()); ns.setDeptId(SecurityUtils.getDeptId());
                ns.setDelStatus(0L); ns.setCreateTime(DateUtils.getNowDate());
                workReportSheetService.insertWorkReportSheet(ns);
            }
            extractAndSaveCells(sheetObj, sheetDbId);
        }
        return success("保存成功");
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
        WorkReportSheet sheet = workReportSheetService.selectWorkReportSheetById(sheetDbId);
        if (sheet == null) return error("Sheet不存在");
        return success(workReportCellService.selectCellsByRange(sheetDbId, startRow, endRow, startCol, endCol));
    }
    @SuppressWarnings("unchecked")
    @PreAuthorize("@ss.hasPermi('collect:report:edit')")
    @Log(title = "报表单元格", businessType = BusinessType.UPDATE)
    @PutMapping("/cells")
    public AjaxResult saveCells(@RequestBody Map<String, Object> body) {
        Object raw = body.get("cells");
        if (raw == null) return success("无变更");
        List<Map<String, Object>> cellList = (List<Map<String, Object>>) raw;
        if (cellList.isEmpty()) return success("无变更");
        List<WorkReportCell> cells = cellList.stream().map(m -> {
            WorkReportCell c = new WorkReportCell();
            c.setSheetId((String) m.get("sheetDbId"));
            c.setRowIndex(m.get("rowIndex") != null ? ((Number) m.get("rowIndex")).intValue() : 0);
            c.setColIndex(m.get("colIndex") != null ? ((Number) m.get("colIndex")).intValue() : 0);
            c.setCellValue((String) m.get("cellValue"));
            c.setCellFormula((String) m.get("cellFormula"));
            c.setCellType((String) m.get("cellType"));
            return c;
        }).collect(Collectors.toList());
        workReportCellService.batchUpsertCells(cells);
        return success("保存成功");
    }

    // ==================== 显式权限分配 ====================
    @PreAuthorize("@ss.hasPermi('collect:report:query')")
    @GetMapping("/permissions/{sheetDbId}")
    public AjaxResult listPermissions(@PathVariable String sheetDbId) {
        if (workReportSheetService.selectWorkReportSheetById(sheetDbId) == null) return error("Sheet不存在");
        return success(permissionService.listBySheet(sheetDbId));
    }
    @PreAuthorize("@ss.hasPermi('collect:report:edit')")
    @Log(title = "权限", businessType = BusinessType.GRANT)
    @PostMapping("/permissions/{sheetDbId}")
    public AjaxResult grantPermission(@PathVariable String sheetDbId, @RequestBody Map<String, Object> body) {
        WorkReportSheet sheet = workReportSheetService.selectWorkReportSheetById(sheetDbId);
        if (sheet == null) return error("Sheet不存在");
        Long currentUserId = SecurityUtils.getUserId();
        if (!currentUserId.equals(sheet.getUserId()) && !SecurityUtils.isAdmin(currentUserId))
            return error("只有创建者和管理员可以分配权限");
        String permType = (String) body.get("permType");
        Number permIdNum = (Number) body.get("permId");
        if (permType == null || permIdNum == null) return error("缺少permType或permId");
        WorkReportSheetPermission p = new WorkReportSheetPermission();
        p.setSheetId(sheetDbId); p.setPermType(permType);
        p.setPermId(permIdNum.longValue()); p.setGrantedBy(currentUserId);
        permissionService.grant(p);
        return success("权限分配成功");
    }
    @PreAuthorize("@ss.hasPermi('collect:report:edit')")
    @Log(title = "权限", businessType = BusinessType.DELETE)
    @DeleteMapping("/permissions/{sheetDbId}")
    public AjaxResult revokePermission(@PathVariable String sheetDbId,
        @RequestParam String permType, @RequestParam Long permId) {
        WorkReportSheet sheet = workReportSheetService.selectWorkReportSheetById(sheetDbId);
        if (sheet == null) return error("Sheet不存在");
        Long currentUserId = SecurityUtils.getUserId();
        if (!currentUserId.equals(sheet.getUserId()) && !SecurityUtils.isAdmin(currentUserId))
            return error("只有创建者和管理员可以撤销权限");
        permissionService.revoke(sheetDbId, permType, permId);
        return success("权限撤销成功");
    }

    // ==================== 内部工具 ====================
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
    @SuppressWarnings("unchecked")
    private void extractAndSaveCells(Map<String, Object> sheetObj, String sheetDbId) {
        Object celldata = sheetObj.get("celldata");
        if (!(celldata instanceof List)) return;
        List<Map<String, Object>> cellList = (List<Map<String, Object>>) celldata;
        if (cellList.isEmpty()) return;
        List<WorkReportCell> cells = cellList.stream().map(m -> {
            WorkReportCell c = new WorkReportCell();
            c.setSheetId(sheetDbId);
            c.setRowIndex(((Number) m.get("r")).intValue());
            c.setColIndex(((Number) m.get("c")).intValue());
            Object v = m.get("v");
            if (v instanceof Map) {
                Map<String, Object> vm = (Map<String, Object>) v;
                Object raw = vm.get("v");
                c.setCellValue(raw != null ? raw.toString() : null);
                c.setCellFormula((String) vm.get("f"));
                c.setCellType(vm.containsKey("f") ? "formula" : (raw instanceof Number ? "number" : "string"));
                try { c.setCellStyle(MAPPER.writeValueAsString(vm)); } catch (Exception e) {
                    log.warn("Cell样式序列化失败 sheetId={}, row={}, col={}", sheetDbId,
                            m.get("r"), m.get("c"), e);
                }
            } else {
                c.setCellValue(v != null ? v.toString() : null);
            }
            return c;
        }).collect(Collectors.toList());
        workReportCellService.batchUpsertCells(cells);
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
