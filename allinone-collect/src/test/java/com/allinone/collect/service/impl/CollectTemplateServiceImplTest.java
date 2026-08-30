package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectTemplate;
import com.allinone.collect.mapper.CollectDataCellMapper;
import com.allinone.collect.mapper.CollectDataMapper;
import com.allinone.collect.mapper.CollectTemplateMapper;
import com.allinone.common.core.redis.RedisCache;
import com.allinone.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectTemplateServiceImplTest {

    private final CollectTemplateMapper mapper = mock(CollectTemplateMapper.class);
    private final CollectDataMapper dataMapper = mock(CollectDataMapper.class);
    private final CollectDataCellMapper cellMapper = mock(CollectDataCellMapper.class);
    private final RedisCache redisCache = mock(RedisCache.class);
    private TestableCollectTemplateService service;

    @BeforeEach
    void setUp() {
        service = new TestableCollectTemplateService();
        ReflectionTestUtils.setField(service, "collectTemplateMapper", mapper);
        ReflectionTestUtils.setField(service, "collectDataMapper", dataMapper);
        ReflectionTestUtils.setField(service, "collectDataCellMapper", cellMapper);
        ReflectionTestUtils.setField(service, "redisCache", redisCache);
    }

    @Test
    void insertAlwaysCreatesDraftTemplate() {
        CollectTemplate template = new CollectTemplate();
        template.setStatus("1");
        when(mapper.insertCollectTemplate(template)).thenReturn(1);

        service.insertCollectTemplate(template);

        assertThat(template.getStatus()).isEqualTo("0");
        assertThat(template.getCreateBy()).isEqualTo("alice");
    }

    @Test
    void ordinaryUpdateCannotChangePublishStatus() {
        CollectTemplate template = new CollectTemplate();
        template.setTemplateId(1L);
        template.setVersion(3);
        template.setStatus("1");
        when(mapper.updateCollectTemplate(template)).thenReturn(1);

        service.updateCollectTemplate(template);

        assertThat(template.getStatus()).isNull();
        verify(mapper).updateCollectTemplate(template);
    }

    @Test
    void insertRejectsDuplicateCodeIncludingSoftDeletedRows() {
        CollectTemplate template = new CollectTemplate();
        template.setTemplateCode("dup_code");
        when(mapper.countTemplateCodeConflict("dup_code", null)).thenReturn(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.insertCollectTemplate(template))
                .isInstanceOf(com.allinone.common.exception.ServiceException.class)
                .hasMessageContaining("模板编码已存在");
        verify(mapper).countTemplateCodeConflict("dup_code", null);
    }

    @Test
    void insertAcceptsUniqueCode() {
        CollectTemplate template = new CollectTemplate();
        template.setTemplateCode("fresh_code");
        when(mapper.countTemplateCodeConflict("fresh_code", null)).thenReturn(0);
        when(mapper.insertCollectTemplate(template)).thenReturn(1);

        service.insertCollectTemplate(template);

        assertThat(template.getStatus()).isEqualTo("0");
        verify(mapper).insertCollectTemplate(template);
    }

    @Test
    void updateChecksCodeConflictExcludingItself() {
        CollectTemplate template = new CollectTemplate();
        template.setTemplateId(5L);
        template.setVersion(2);
        template.setTemplateCode("kept_code");
        when(mapper.countTemplateCodeConflict("kept_code", 5L)).thenReturn(0);
        when(mapper.updateCollectTemplate(template)).thenReturn(1);

        service.updateCollectTemplate(template);

        verify(mapper).countTemplateCodeConflict("kept_code", 5L);
        verify(mapper).updateCollectTemplate(template);
    }

    @Test
    void copyClonesDefinitionAsUnpublishedDraft() {
        CollectTemplate src = new CollectTemplate();
        src.setTemplateId(5L);
        src.setTemplateName("月度预算表");
        src.setTemplateCode("monthly_budget");
        src.setTemplateJson("[{}]");
        src.setStatus("1");
        src.setVersion(3);
        when(mapper.selectCollectTemplateById(5L)).thenReturn(src);
        when(mapper.countTemplateCodeConflict(any(), any())).thenReturn(0);
        when(mapper.insertCollectTemplate(any())).thenReturn(1);

        CollectTemplate copy = service.copyTemplate(5L);

        assertThat(copy.getTemplateId()).isNotNull().isNotEqualTo(5L);
        assertThat(copy.getTemplateName()).isEqualTo("月度预算表-副本");
        assertThat(copy.getTemplateCode()).startsWith("monthly_budget_copy_");
        assertThat(copy.getTemplateJson()).isEqualTo("[{}]");
        assertThat(copy.getStatus()).isEqualTo("0");
        assertThat(copy.getVersion()).isEqualTo(1);
        verify(mapper).insertCollectTemplate(copy);
    }

    @Test
    void copyRejectsMissingTemplate() {
        when(mapper.selectCollectTemplateById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.copyTemplate(404L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("模板不存在");
    }

    @Test
    void deleteCascadesDraftDataAndCellSnapshots() {
        when(mapper.countSubmittedDataByTemplateIds(any())).thenReturn(0);
        when(mapper.countFieldMappingByTemplateIds(any())).thenReturn(0);
        when(dataMapper.selectDraftDataIdsByTemplateIds(any())).thenReturn(List.of(101L, 102L));
        when(mapper.deleteCollectTemplateByIds(any())).thenReturn(1);

        service.deleteCollectTemplateByIds(new Long[] {9L});

        verify(cellMapper).deleteCollectDataCellByDataIds(List.of(101L, 102L));
        verify(dataMapper).deleteDraftDataByTemplateIds(any());
        verify(mapper).deleteCollectTemplateByIds(any());
    }

    @Test
    void deleteWithoutDraftsSkipsCascade() {
        when(mapper.countSubmittedDataByTemplateIds(any())).thenReturn(0);
        when(mapper.countFieldMappingByTemplateIds(any())).thenReturn(0);
        when(dataMapper.selectDraftDataIdsByTemplateIds(any())).thenReturn(List.of());
        when(mapper.deleteCollectTemplateByIds(any())).thenReturn(1);

        service.deleteCollectTemplateByIds(new Long[] {9L});

        verify(cellMapper, never()).deleteCollectDataCellByDataIds(any());
        verify(dataMapper, never()).deleteDraftDataByTemplateIds(any());
    }

    private static final class TestableCollectTemplateService extends CollectTemplateServiceImpl {
        @Override
        protected String currentUsername() {
            return "alice";
        }
    }
}
