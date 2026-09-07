package com.allinone.supply.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 真实 Flowable 引擎集成测试：以内存 H2 启动引擎并部署默认采购订单 BPMN，
 * 验证「启动→主管待办→通过/退回」在引擎层的语义，作为 S1-S4 修复的回归基线。
 *
 * 注意：本测试不依赖业务库与 Spring 上下文，可在 CI 的 {@code mvn -pl allinone-supply -am test} 中执行。
 */
class WorkflowBpmnEngineIntegrationTest {

    private ProcessEngine engine;

    @BeforeEach
    void setUp() {
        engine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .buildProcessEngine();
        engine.getRepositoryService().createDeployment()
                .addClasspathResource("processes/purchase-order-approval.bpmn20.xml")
                .deploy();
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    void startCreatesSupervisorTaskWithConfiguredAssignee() {
        ProcessInstance instance = start("200");

        Task task = engine.getTaskService().createTaskQuery()
                .processInstanceId(instance.getId()).singleResult();

        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("supervisorApproval");
        assertThat(task.getAssignee()).isEqualTo("200");
    }

    @Test
    void approveEndsInstanceAndRejectDeletesInstance() {
        // 通过：任务完成后流程进入结束事件，历史实例带结束时间。
        ProcessInstance approved = start("200");
        Task approveTask = engine.getTaskService().createTaskQuery()
                .processInstanceId(approved.getId()).singleResult();
        engine.getTaskService().complete(approveTask.getId(),
                Map.of("approvalAction", "APPROVE", "approvalComment", "同意"));
        HistoricProcessInstance approvedHistory = engine.getHistoryService()
                .createHistoricProcessInstanceQuery().processInstanceId(approved.getId()).singleResult();
        assertThat(approvedHistory.getEndTime()).isNotNull();

        // 退回：实现为删除运行中实例（与 WorkflowEngineServiceImpl 的 REJECT 语义一致），无残留待办。
        ProcessInstance rejected = start("200");
        engine.getRuntimeService().deleteProcessInstance(rejected.getId(), "REJECTED");
        assertThat(engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(rejected.getId()).count()).isZero();
        assertThat(engine.getHistoryService().createHistoricProcessInstanceQuery()
                .processInstanceId(rejected.getId()).singleResult().getEndTime()).isNotNull();
    }

    private ProcessInstance start(String approverUserId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("documentId", "1");
        variables.put("documentNumber", "PO-TEST-1");
        variables.put("documentVersionId", "11");
        variables.put("startedBy", "100");
        variables.put("approverUserId", approverUserId);
        return engine.getRuntimeService().startProcessInstanceByKey(
                WorkflowEngineServiceImpl.PURCHASE_ORDER_PROCESS_KEY, "1", variables);
    }
}
