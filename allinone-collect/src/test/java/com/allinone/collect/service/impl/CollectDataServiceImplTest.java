package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectDataCell;
import com.allinone.collect.mapper.CollectDataCellMapper;
import com.allinone.collect.mapper.CollectDataMapper;
import com.allinone.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

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
