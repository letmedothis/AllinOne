package com.allinone.supply.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.allinone.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

class WorkflowBpmnSupportTest {
    @Test
    void parsesRoleAssignment() {
        String xml = xml("flowable:candidateGroups=\"finance\" allinone:assigneeType=\"ROLE\" allinone:assigneeValue=\"finance\"");
        WorkflowBpmnSupport.ParsedBpmn parsed = WorkflowBpmnSupport.parseAndValidate(xml, "purchase-order-approval");
        assertThat(parsed.assignments()).singleElement().satisfies(item -> {
            assertThat(item.type()).isEqualTo("ROLE");
            assertThat(item.value()).isEqualTo("finance");
        });
    }

    @Test
    void rejectsTaskWithoutManagedAssignment() {
        assertThatThrownBy(() -> WorkflowBpmnSupport.parseAndValidate(xml(""), "purchase-order-approval"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("配置审批人类型");
    }

    private String xml(String attributes) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:flowable=\"http://flowable.org/bpmn\" xmlns:allinone=\"https://allinone.local/workflow\"><process id=\"purchase-order-approval\" name=\"采购订单审批\"><userTask id=\"approve\" name=\"审批\" " + attributes + "/></process></definitions>";
    }
}
