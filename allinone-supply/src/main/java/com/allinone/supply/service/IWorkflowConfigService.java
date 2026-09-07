package com.allinone.supply.service;
import com.allinone.supply.domain.WorkflowConfig; import java.util.List;
public interface IWorkflowConfigService { List<WorkflowConfig> list(); int save(WorkflowConfig config); }
