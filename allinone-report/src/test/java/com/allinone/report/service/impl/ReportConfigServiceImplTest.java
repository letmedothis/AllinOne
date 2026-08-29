package com.allinone.report.service.impl;

import com.allinone.common.core.domain.model.LoginUser;
import com.allinone.report.domain.ReportConfig;
import com.allinone.report.mapper.ReportConfigMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 插入逻辑会写入 create_by（SecurityUtils.getUsername()），需要登录上下文 */
    private void setLoginContext() {
        LoginUser loginUser = mock(LoginUser.class);
        when(loginUser.getUsername()).thenReturn("admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    @Test
    void insertGeneratesReportIdBeforePersisting() {
        ReportConfig config = new ReportConfig();
        config.setReportName("销售汇总");
        config.setReportCode("sales_summary");
        setLoginContext();
        when(mapper.insertReportConfig(config)).thenReturn(1);

        int rows = service.insertReportConfig(config);

        assertThat(rows).isEqualTo(1);
        assertThat(config.getReportId()).isNotNull().isPositive();
        assertThat(config.getReportId()).isLessThanOrEqualTo(9_007_199_254_740_991L);
        verify(mapper).insertReportConfig(config);
    }

    @Test
    void insertRejectsDuplicateCodeIncludingSoftDeletedRows() {
        ReportConfig config = new ReportConfig();
        config.setReportCode("dup_code");
        setLoginContext();
        when(mapper.countReportCodeConflict("dup_code", null)).thenReturn(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.insertReportConfig(config))
                .isInstanceOf(com.allinone.common.exception.ServiceException.class)
                .hasMessageContaining("报表编码已存在");
        verify(mapper).countReportCodeConflict("dup_code", null);
    }

    @Test
    void updateChecksCodeConflictExcludingItself() {
        ReportConfig config = new ReportConfig();
        config.setReportId(7L);
        config.setReportCode("kept_code");
        setLoginContext();
        when(mapper.countReportCodeConflict("kept_code", 7L)).thenReturn(0);
        when(mapper.updateReportConfig(config)).thenReturn(1);

        service.updateReportConfig(config);

        verify(mapper).countReportCodeConflict("kept_code", 7L);
        verify(mapper).updateReportConfig(config);
    }
}
