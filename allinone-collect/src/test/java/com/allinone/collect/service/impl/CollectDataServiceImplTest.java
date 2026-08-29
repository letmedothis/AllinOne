package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectDataCell;
import com.allinone.collect.mapper.CollectDataCellMapper;
import com.allinone.collect.mapper.CollectDataMapper;
import com.allinone.common.exception.ServiceException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectDataServiceImplTest {

    private final CollectDataMapper dataMapper = mock(CollectDataMapper.class);
    private final CollectDataCellMapper cellMapper = mock(CollectDataCellMapper.class);
    private TestableCollectDataService service;

    @BeforeEach
    void setUp() {
        service = new TestableCollectDataService();
        ReflectionTestUtils.setField(service, "collectDataMapper", dataMapper);
        ReflectionTestUtils.setField(service, "collectDataCellMapper", cellMapper);
    }

    @Test
    void insertAssignsJavascriptSafeIdAndAuditFields() {
        CollectData data = new CollectData();
        data.setTemplateId(7L);
        data.setFormData("[]");
        data.setDeptId(999L);
        data.setSubmitBy("mallory");
        when(dataMapper.insertCollectData(data)).thenReturn(1);

        service.insertCollectData(data);

        assertThat(data.getDataId()).isPositive().isLessThanOrEqualTo(9_007_199_254_740_991L);
        assertThat(data.getBizStatus()).isEqualTo("draft");
        assertThat(data.getVersion()).isEqualTo(1);
        assertThat(data.getCreateBy()).isEqualTo("alice");
        assertThat(data.getDeptId()).isEqualTo(10L);
        assertThat(data.getSubmitBy()).isNull();
        assertThat(data.getSubmitTime()).isNull();
    }

    @Test
    void parsesCellsFromEveryLuckysheetSheet() {
        String workbook = "[{\"index\":\"sheet-a\",\"celldata\":[{\"r\":1,\"c\":2,\"v\":{\"v\":12,\"m\":\"12\"}}]},"
            + "{\"index\":\"sheet-b\",\"celldata\":[{\"r\":3,\"c\":4,\"v\":{\"v\":\"ok\",\"m\":\"ok\"}}]}]";

        List<CollectDataCell> cells = service.parseLuckysheetJson(workbook);

        assertThat(cells).hasSize(2);
        assertThat(cells.get(0).getSheetIndex()).isZero();
        assertThat(cells.get(0).getRowIndex()).isEqualTo(1);
        assertThat(cells.get(0).getColIndex()).isEqualTo(2);
        assertThat(cells.get(0).getCellValue()).isEqualTo("12");
        assertThat(cells.get(1).getSheetIndex()).isEqualTo(1);
    }

    @Test
    void submitDoesNotMarkSubmittedWhenCellPersistenceFails() {
        CollectData data = draftOwnedByAlice();
        data.setFormData("[{\"celldata\":[{\"r\":0,\"c\":0,\"v\":{\"v\":\"x\",\"m\":\"x\"}}]}]");
        when(dataMapper.selectCollectDataById(1L)).thenReturn(data);
        doThrow(new IllegalStateException("database unavailable")).when(cellMapper).batchUpsert(any());

        assertThatThrownBy(() -> service.submitData(1L)).isInstanceOf(IllegalStateException.class);
        verify(cellMapper).deleteCollectDataCellByDataId(1L);
        verify(dataMapper, never()).updateCollectDataStatus(any());
    }

    @Test
    void submitReplacesExistingCellsWithAnEmptySnapshot() {
        CollectData data = draftOwnedByAlice();
        data.setFormData("[]");
        when(dataMapper.selectCollectDataById(1L)).thenReturn(data);
        when(dataMapper.updateCollectDataStatus(data)).thenReturn(1);

        service.submitData(1L);

        verify(cellMapper).deleteCollectDataCellByDataId(1L);
        verify(cellMapper, never()).batchUpsert(any());
        verify(dataMapper).updateCollectDataStatus(data);
    }

    @Test
    void updateRejectsDataOwnedByAnotherUser() {
        CollectData existing = draftOwnedByAlice();
        existing.setCreateBy("bob");
        when(dataMapper.selectCollectDataById(1L)).thenReturn(existing);

        CollectData update = new CollectData();
        update.setDataId(1L);
        update.setVersion(1);

        assertThatThrownBy(() -> service.updateCollectData(update))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("无权");
    }

    @Test
    void ordinaryUpdateCannotChangeBusinessStatus() {
        CollectData existing = draftOwnedByAlice();
        when(dataMapper.selectCollectDataById(1L)).thenReturn(existing);

        CollectData update = new CollectData();
        update.setDataId(1L);
        update.setVersion(1);
        update.setBizStatus("submitted");
        when(dataMapper.updateCollectData(update)).thenReturn(1);

        service.updateCollectData(update);

        assertThat(update.getBizStatus()).isNull();
        verify(dataMapper).updateCollectData(update);
    }

    @Test
    void deletingDraftAlsoDeletesItsCellSnapshot() {
        CollectData data = draftOwnedByAlice();
        when(dataMapper.selectCollectDataById(1L)).thenReturn(data);
        when(dataMapper.deleteCollectDataByIds(any())).thenReturn(1);

        service.deleteCollectDataByIds(new Long[] {1L});

        verify(cellMapper).deleteCollectDataCellByDataId(1L);
    }

    @Test
    void exportBuildsPerRecordSheetsWithCellValueGrid() throws IOException {
        CollectData query = new CollectData();
        CollectData single = submittedRecord(1L, "TPL_A");
        CollectData multi = submittedRecord(2L, "TPL_B");
        when(dataMapper.selectCollectDataList(query)).thenReturn(new ArrayList<>(Arrays.asList(single, multi)));
        // 快照故意乱序返回，验证按坐标重建网格
        when(cellMapper.countCollectDataCellByDataIds(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(
            cellCount(1L, 3), cellCount(2L, 2)));
        when(cellMapper.selectCollectDataCellByDataId(1L)).thenReturn(Arrays.asList(
            cell(1L, 0, 1, 1, "张三"),
            cell(1L, 0, 1, 0, "姓名"),
            cell(1L, 0, 2, 1, "42")));
        when(cellMapper.selectCollectDataCellByDataId(2L)).thenReturn(Arrays.asList(
            cell(2L, 0, 0, 0, "甲"),
            cell(2L, 1, 0, 0, "乙")));

        XSSFWorkbook wb = exportAndReopen(service.exportWorkbook(query));

        try {
            assertThat(wb.getNumberOfSheets()).isEqualTo(4);
            // 第一个工作表：元数据汇总
            Sheet summary = wb.getSheet("填报记录");
            assertThat(summary).isNotNull();
            assertThat(summary.getRow(0).getCell(0).getStringCellValue()).isEqualTo("模板名称");
            assertThat(summary.getLastRowNum()).isEqualTo(2);
            // 单 Sheet 记录：工作表名“序号-模板编码”
            Sheet grid1 = wb.getSheet("1-TPL_A");
            assertThat(grid1).isNotNull();
            assertThat(grid1.getRow(1).getCell(0).getStringCellValue()).isEqualTo("姓名");
            assertThat(grid1.getRow(1).getCell(1).getStringCellValue()).isEqualTo("张三");
            assertThat(grid1.getRow(2).getCell(1).getStringCellValue()).isEqualTo("42");
            // 未填值的坐标不创建单元格
            assertThat(grid1.getRow(2).getCell(0)).isNull();
            // 多 Sheet 记录：按 sheet_index 分组为多个工作表，名称追加“-S{sheetIndex}”
            assertThat(wb.getSheet("2-TPL_B-S0").getRow(0).getCell(0).getStringCellValue()).isEqualTo("甲");
            assertThat(wb.getSheet("2-TPL_B-S1").getRow(0).getCell(0).getStringCellValue()).isEqualTo("乙");
            // 正常导出的记录不写备注
            assertThat(single.getExportNote()).isNull();
            assertThat(multi.getExportNote()).isNull();
        } finally {
            wb.close();
        }
    }

    @Test
    void exportRejectsWhenResultExceedsLimit() {
        CollectData query = new CollectData();
        List<CollectData> list = new ArrayList<>();
        for (long i = 1; i <= 201; i++) {
            list.add(submittedRecord(i, "TPL_" + i));
        }
        when(dataMapper.selectCollectDataList(query)).thenReturn(list);

        assertThatThrownBy(() -> service.exportWorkbook(query))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("导出数据过多");
        verify(cellMapper, never()).selectCollectDataCellByDataId(any());
    }

    @Test
    void exportSkipsRecordsWithoutCellSnapshot() throws IOException {
        CollectData query = new CollectData();
        CollectData empty = submittedRecord(1L, "TPL_EMPTY");
        when(dataMapper.selectCollectDataList(query)).thenReturn(new ArrayList<>(List.of(empty)));
        when(cellMapper.countCollectDataCellByDataIds(List.of(1L))).thenReturn(List.of());

        XSSFWorkbook wb = exportAndReopen(service.exportWorkbook(query));

        try {
            // 无快照记录跳过，仅返回“填报记录”汇总表
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            assertThat(wb.getSheet("填报记录")).isNotNull();
            assertThat(empty.getExportNote()).isEqualTo("无填报内容");
            // 备注列写入汇总表（第 7 列，数据从第 2 行开始）
            Row dataRow = wb.getSheet("填报记录").getRow(1);
            assertThat(dataRow.getCell(6).getStringCellValue()).isEqualTo("无填报内容");
        } finally {
            wb.close();
        }
    }

    @Test
    void exportSkipsOversizedSnapshotWithNote() throws IOException {
        CollectData query = new CollectData();
        CollectData oversized = submittedRecord(1L, "TPL_BIG");
        // 计数超限（50001 > 50000）即标记“内容过大未导出”，快照不再加载
        when(dataMapper.selectCollectDataList(query)).thenReturn(new ArrayList<>(List.of(oversized)));
        when(cellMapper.countCollectDataCellByDataIds(List.of(1L))).thenReturn(List.of(cellCount(1L, 50001)));

        XSSFWorkbook wb = exportAndReopen(service.exportWorkbook(query));

        try {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            assertThat(wb.getSheet("填报记录")).isNotNull();
            assertThat(oversized.getExportNote()).isEqualTo("内容过大未导出");
        } finally {
            wb.close();
        }
    }

    @Test
    void exportSanitizesAndTruncatesDataSheetName() throws IOException {
        CollectData query = new CollectData();
        CollectData record = submittedRecord(1L, "A/B\\C:D*E?F[G]H_0123456789012345678901");
        when(dataMapper.selectCollectDataList(query)).thenReturn(new ArrayList<>(List.of(record)));
        when(cellMapper.countCollectDataCellByDataIds(List.of(1L))).thenReturn(List.of(cellCount(1L, 1)));
        when(cellMapper.selectCollectDataCellByDataId(1L)).thenReturn(List.of(cell(1L, 0, 0, 0, "v")));

        XSSFWorkbook wb = exportAndReopen(service.exportWorkbook(query));

        try {
            String sheetName = wb.getSheetName(1);
            assertThat(sheetName).doesNotContain("/", "\\", ":", "*", "?", "[", "]");
            assertThat(sheetName.length()).isLessThanOrEqualTo(31);
            assertThat(sheetName).isEqualTo("1-A_B_C_D_E_F_G_H_0123456789012");
        } finally {
            wb.close();
        }
    }

    /** 将导出的流式工作簿真实写出，再以 XSSFWorkbook 重新打开用于断言 */
    private XSSFWorkbook exportAndReopen(SXSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        IOUtils.closeQuietly(wb);
        wb.dispose();
        return new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()));
    }

    private CollectData submittedRecord(Long dataId, String templateCode) {
        CollectData data = new CollectData();
        data.setDataId(dataId);
        data.setTemplateName("模板" + dataId);
        data.setTemplateCode(templateCode);
        data.setBizStatus("submitted");
        data.setCreateBy("alice");
        return data;
    }

    private CollectDataCell cell(Long dataId, int sheetIndex, int rowIndex, int colIndex, String value) {
        CollectDataCell cell = new CollectDataCell();
        cell.setDataId(dataId);
        cell.setTemplateId(7L);
        cell.setSheetIndex(sheetIndex);
        cell.setRowIndex(rowIndex);
        cell.setColIndex(colIndex);
        cell.setCellValue(value);
        return cell;
    }

    /** 模拟 countCollectDataCellByDataIds 的计数行（dataId/cellCount） */
    private static Map<String, Object> cellCount(long dataId, long count) {
        Map<String, Object> row = new HashMap<>();
        row.put("dataId", dataId);
        row.put("cellCount", count);
        return row;
    }

    private CollectData draftOwnedByAlice() {
        CollectData data = new CollectData();
        data.setDataId(1L);
        data.setTemplateId(7L);
        data.setBizStatus("draft");
        data.setVersion(1);
        data.setCreateBy("alice");
        return data;
    }

    private static final class TestableCollectDataService extends CollectDataServiceImpl {
        @Override
        protected String currentUsername() {
            return "alice";
        }

        @Override
        protected Long currentDeptId() {
            return 10L;
        }

        @Override
        protected boolean currentUserIsAdmin() {
            return false;
        }
    }
}
