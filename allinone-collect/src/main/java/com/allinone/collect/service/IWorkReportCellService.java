package com.allinone.collect.service;

import com.allinone.collect.domain.WorkReportCell;
import java.util.List;

public interface IWorkReportCellService
{
    List<WorkReportCell> selectCellsByRange(String sheetId, int startRow, int endRow, int startCol, int endCol);
    int batchUpsertCells(List<WorkReportCell> cells);
    int deleteCellsBySheetId(String sheetId);
    int deleteCellsByReportId(String reportId);
}
