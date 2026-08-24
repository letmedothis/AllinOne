package com.allinone.collect.mapper;

import com.allinone.collect.domain.WorkReportCell;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface WorkReportCellMapper
{
    List<WorkReportCell> selectCellsByRange(
        @Param("sheetId") String sheetId,
        @Param("startRow") int startRow,
        @Param("endRow") int endRow,
        @Param("startCol") int startCol,
        @Param("endCol") int endCol);
    int batchUpsertCells(List<WorkReportCell> cells);
    int deleteCellsBySheetId(@Param("sheetId") String sheetId);
    int deleteCellsBySheetIds(@Param("sheetIds") String[] sheetIds);
    int deleteCellsByReportId(@Param("reportId") String reportId);
    int deleteCellsByRange(
        @Param("sheetId") String sheetId,
        @Param("startRow") int startRow,
        @Param("endRow") int endRow,
        @Param("startCol") int startCol,
        @Param("endCol") int endCol);
}
