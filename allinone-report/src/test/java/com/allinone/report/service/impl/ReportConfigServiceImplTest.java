package com.allinone.report.service.impl;

import com.allinone.report.domain.ReportConfig;
import com.allinone.report.mapper.ReportConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportConfigServiceImplTest {

    private final ReportConfigMapper mapper = mock(ReportConfigMapper.class);
    private ReportConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportConfigServiceImpl();
        ReflectionTestUtils.setField(service, "reportConfigMapper", mapper);
    }

    @Test
    void insertGeneratesReportIdBeforePersisting() {
        ReportConfig config = new ReportConfig();
        config.setReportName("销售汇总");
        config.setReportCode("sales_summary");
        when(mapper.insertReportConfig(config)).thenReturn(1);

        int rows = service.insertReportConfig(config);

        assertThat(rows).isEqualTo(1);
        assertThat(config.getReportId()).isNotNull().isPositive();
        assertThat(config.getReportId()).isLessThanOrEqualTo(9_007_199_254_740_991L);
        verify(mapper).insertReportConfig(config);
    }
}
