package com.allinone.collect.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.allinone.collect.mapper.WorkReportCellMapper;
import com.allinone.collect.domain.WorkReportCell;
import com.allinone.collect.service.IWorkReportCellService;

@Service
public class WorkReportCellServiceImpl implements IWorkReportCellService {

    @Autowired
    private WorkReportCellMapper workReportCellMapper;

    @Override
    public List<WorkReportCell> selectCellsByRange(String sheetId, int startRow, int endRow, int startCol, int endCol) {
        return workReportCellMapper.selectCellsByRange(sheetId, startRow, endRow, startCol, endCol);
    }

    @Override
    public int batchUpsertCells(List<WorkReportCell> cells) {
        for (WorkReportCell cell : cells) {
            cell.setUpdateTime(new Date());
            if (cell.getId() == null) {
                cell.setCreateTime(new Date());
            }
        }
        return workReportCellMapper.batchUpsertCells(cells);
    }

    @Override
    public int deleteCellsBySheetId(String sheetId) {
        return workReportCellMapper.deleteCellsBySheetId(sheetId);
    }

    @Override
    public int deleteCellsByReportId(String reportId) {
        return workReportCellMapper.deleteCellsByReportId(reportId);
    }
}
