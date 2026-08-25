package com.allinone.collect.controller;

import com.allinone.collect.domain.CollectCategory;
import com.allinone.collect.service.ICollectCategoryService;
import com.allinone.common.core.domain.AjaxResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollectCategoryControllerTest {

    private final ICollectCategoryService service = mock(ICollectCategoryService.class);
    private CollectCategoryController controller;

    @BeforeEach
    void setUp() {
        controller = new TestableCollectCategoryController();
        ReflectionTestUtils.setField(controller, "collectCategoryService", service);
    }

    @Test
    void listReturnsCategoriesInAjaxResultData() {
        CollectCategory query = new CollectCategory();
        CollectCategory category = new CollectCategory();
        category.setCategoryId(1L);
        List<CollectCategory> categories = List.of(category);
        when(service.selectCollectCategoryList(query)).thenReturn(categories);

        Object response = controller.list(query);

        assertThat(response).isInstanceOf(AjaxResult.class);
        assertThat(((AjaxResult) response).get(AjaxResult.DATA_TAG)).isEqualTo(categories);
    }

    private static final class TestableCollectCategoryController extends CollectCategoryController {
        @Override
        protected void startPage() {
            // Avoid servlet pagination setup while reproducing the response contract.
        }
    }
}
