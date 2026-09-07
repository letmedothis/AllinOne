package com.allinone.supply.mapper;
import com.allinone.supply.domain.WorkflowConfig; import java.util.List;
public interface WorkflowConfigMapper { List<WorkflowConfig> selectAll(); WorkflowConfig selectLatest(String flowType); int insert(WorkflowConfig config); }
