package com.allinone.framework.approval.mapper;

import com.allinone.common.approval.*;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface ReviewMapper {
    ReviewCase get(@Param("id") Long id);
    ReviewCase lock(@Param("id") Long id);
    List<ReviewCase> list(@Param("type") String type,@Param("userId") Long userId,@Param("admin") boolean admin);
    int insert(ReviewCase request);
    int update(ReviewCase request);
    ApprovalUser user(@Param("id") Long id);
    ApprovalUser lockUser(@Param("id") Long id);
    List<ApprovalUser> users();
    int permission(@Param("id") Long id,@Param("permission") String permission);
    int role(@Param("id") Long id,@Param("role") String role);
    ReviewConfig config(@Param("type") String type,@Param("targetKey") Long targetKey,@Param("deptId") Long deptId);
    List<ReviewConfig> configs();
    int saveConfig(ReviewConfig config);
    int event(@Param("id") Long id,@Param("request") ReviewCase request,@Param("action") String action,
              @Param("actor") ApprovalActor actor,@Param("comment") String comment,@Param("key") String key);
    int hasEvent(@Param("caseId") Long caseId,@Param("key") String key,@Param("actorId") Long actorId);
    List<Map<String,Object>> events(@Param("id") Long id);
    int insertFile(@Param("id") Long id,@Param("caseId") Long caseId,@Param("name") String name,@Param("path") String path,@Param("hash") String hash,@Param("userId") Long userId);
    List<Map<String,Object>> files(@Param("id") Long id);
    Map<String,Object> file(@Param("id") Long id,@Param("fileId") Long fileId);
    List<DelegationGrant> grants(@Param("agentId") Long agentId);
    DelegationGrant grant(@Param("id") Long id);
    DelegationGrant lockGrant(@Param("id") Long id);
    int insertGrant(DelegationGrant grant);
    int replaceGrant(DelegationGrant grant);
    int grantStatus(@Param("id") Long id,@Param("status") String status);
    int overlapping(DelegationGrant grant);
    int chain(DelegationGrant grant);
    int recordActor(@Param("id") Long id,@Param("type") String type,@Param("businessId") String businessId,
                    @Param("round") String round,@Param("node") String node,@Param("actor") ApprovalActor actor,@Param("action") String action);
    int actorConflict(@Param("type") String type,@Param("businessId") String businessId,@Param("round") String round,
                      @Param("node") String node,@Param("person") Long person);
}
