package com.allinone.collect.service.impl;

import com.allinone.collect.domain.WorkReportSheet;
import com.allinone.collect.service.IWorkReportCellService;
import com.allinone.collect.service.IWorkReportSheetService;
import com.allinone.collect.service.WorkReportAccessService;
import com.allinone.collect.service.WorkReportSheetWriteService;
import com.allinone.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkReportSheetWriteServiceTest {

    private final IWorkReportSheetService sheetService = mock(IWorkReportSheetService.class);
    private final IWorkReportCellService cellService = mock(IWorkReportCellService.class);
    private final WorkReportAccessService accessService = mock(WorkReportAccessService.class);
    private WorkReportSheetWriteService service;

    @BeforeEach
    void setUp() {
        service = new WorkReportSheetWriteService();
        ReflectionTestUtils.setField(service, "workReportSheetService", sheetService);
        ReflectionTestUtils.setField(service, "workReportCellService", cellService);
        ReflectionTestUtils.setField(service, "workReportAccessService", accessService);
    }

    @Test
    void saveCellsRejectsWhenClientVersionIsStale() {
        when(accessService.requireEditableSheet("s1")).thenReturn(new WorkReportSheet());
        when(sheetService.compareAndIncrementVersion(eq("s1"), eq(5L), any())).thenReturn(0);

        Map<String, Object> body = new HashMap<>();
        body.put("cells", List.of(cellBody("s1")));
        body.put("sheetVersions", Map.of("s1", 5));

        assertThatThrownBy(() -> service.saveCells(body))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已被他人修改");
        // 版本冲突时不得写入任何单元格，整体失败由事务回滚保证
        verify(cellService, never()).batchUpsertCells(any());
    }

    @Test
    void saveCellsBumpsVersionAndReturnsNewVersion() {
        when(accessService.requireEditableSheet("s1")).thenReturn(new WorkReportSheet());
        when(sheetService.compareAndIncrementVersion(eq("s1"), eq(5L), any())).thenReturn(1);

        Map<String, Object> body = new HashMap<>();
        body.put("cells", List.of(cellBody("s1")));
        body.put("sheetVersions", Map.of("s1", 5));

        Map<String, Long> result = service.saveCells(body);

        assertThat(result).containsEntry("s1", 6L);
        verify(sheetService).compareAndIncrementVersion(eq("s1"), eq(5L), any());
        verify(cellService, times(1)).batchUpsertCells(any());
    }

    @Test
    void saveCellsWithoutVersionsKeepsLegacyBehavior() {
        when(accessService.requireEditableSheet("s1")).thenReturn(new WorkReportSheet());

        Map<String, Object> body = new HashMap<>();
        body.put("cells", List.of(cellBody("s1")));

        Map<String, Long> result = service.saveCells(body);

        // 旧前端不携带版本号：不做 CAS，兼容升级窗口
        assertThat(result).isEmpty();
        verify(sheetService, never()).compareAndIncrementVersion(any(), anyLong(), any());
        verify(cellService, times(1)).batchUpsertCells(any());
    }

    @Test
    void saveCellsRejectsEmptyAndOversizedPayload() {
        assertThat(service.saveCells(Map.of("cells", List.of()))).isEmpty();

        List<Map<String, Object>> oversized = new java.util.ArrayList<>();
        for (int i = 0; i < 5001; i++) {
            oversized.add(cellBody("s1"));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("cells", oversized);
        assertThatThrownBy(() -> service.saveCells(body))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("5000");
        verify(accessService, never()).requireEditableSheet(any());
    }

    private static Map<String, Object> cellBody(String sheetDbId) {
        Map<String, Object> cell = new HashMap<>();
        cell.put("sheetDbId", sheetDbId);
        cell.put("rowIndex", 0);
        cell.put("colIndex", 0);
        cell.put("cellValue", "x");
        cell.put("cellFormula", null);
        cell.put("cellType", "string");
        return cell;
    }
}
