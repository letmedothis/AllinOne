package com.allinone.supply.mapper;

import com.allinone.supply.domain.WorkflowActionLog;
import com.allinone.supply.domain.WorkflowCandidateOption;
import com.allinone.supply.domain.WorkflowDesign;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface WorkflowEngineMapper {
    WorkflowDesign selectDesignByProcessKey(@Param("processKey") String processKey);
    int insertDesign(WorkflowDesign design);
    int updateDesignDraft(WorkflowDesign design);
    int updateDesignPublished(@Param("id") String id, @Param("revision") Integer revision,
                              @Param("definitionId") String definitionId, @Param("version") Integer version,
                              @Param("updateBy") String updateBy, @Param("publishedAt") Date publishedAt);
    List<WorkflowCandidateOption> selectUserOptions();
    List<WorkflowCandidateOption> selectRoleOptions();
    List<WorkflowCandidateOption> selectDepartmentLeaderOptions();
    int countActiveUser(@Param("userId") String userId);
    int countActiveRole(@Param("roleKey") String roleKey);
    String selectLeaderUserIdByDeptId(@Param("deptId") String deptId);
    String selectInitiatorLeaderUserId(@Param("userId") Long userId);
    List<String> selectUserRoleKeys(@Param("userId") Long userId);
    String selectUserDisplayName(@Param("userId") String userId);
    Long selectDocumentCreatorId(@Param("documentId") Long documentId);
    int insertActionLog(WorkflowActionLog log);
    List<WorkflowActionLog> selectActionLogs(@Param("processInstanceId") String processInstanceId);
}
