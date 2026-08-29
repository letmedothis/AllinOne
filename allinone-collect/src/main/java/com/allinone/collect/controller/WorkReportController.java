package com.allinone.collect.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.allinone.common.annotation.Log;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.enums.BusinessType;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.common.utils.poi.ExcelUtil;
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
    @Autowired private WorkReportSheetWriteService workReportSheetWriteService;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_RANGE_CELLS = 10000;

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
    @PreAuthorize("@ss.hasPermi('collect:report:edit')")
    // 请求体含全部 Sheet 元数据，不落库到操作日志
    @Log(title = "报表Sheet", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    @PutMapping("/sheet/{reportId}")
    public AjaxResult saveSheet(@PathVariable String reportId, @RequestBody Map<String, Object> body) {
        // 事务边界在 WorkReportSheetWriteService 内（含报文校验与写入）
        return success(workReportSheetWriteService.saveSheet(reportId, body));
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
    // 请求体最多 5000 个单元格（每个含完整单元格 JSON），不落库到操作日志
    @Log(title = "报表单元格", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    @PutMapping("/cells")
    public AjaxResult saveCells(@RequestBody Map<String, Object> body) {
        // 事务与乐观锁冲突检测在 WorkReportSheetWriteService 内；返回各 Sheet 递增后的最新版本号
        return success(workReportSheetWriteService.saveCells(body));
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
    private ObjectNode buildLightweightMeta(WorkReportSheet sheet) {
        ObjectNode m = MAPPER.createObjectNode();
        m.put("_sheetDbId", sheet.getId());
        m.put("name", sheet.getSheetName());
        int idx = sheet.getSheetIndex() != null ? sheet.getSheetIndex().intValue() : 0;
        m.put("index", idx); m.put("order", idx);
        m.put("status", idx == 0 ? "1" : "0");
        // 乐观锁版本号：前端保存单元格时回传做冲突检测
        m.put("version", sheet.getVersion() == null ? 0L : sheet.getVersion());
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
