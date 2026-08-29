package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectTemplate;
import com.allinone.collect.mapper.CollectTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectTemplateServiceImplTest {

    private final CollectTemplateMapper mapper = mock(CollectTemplateMapper.class);
    private TestableCollectTemplateService service;

    @BeforeEach
    void setUp() {
        service = new TestableCollectTemplateService();
        ReflectionTestUtils.setField(service, "collectTemplateMapper", mapper);
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

    private static final class TestableCollectTemplateService extends CollectTemplateServiceImpl {
        @Override
        protected String currentUsername() {
            return "alice";
        }
    }
}
