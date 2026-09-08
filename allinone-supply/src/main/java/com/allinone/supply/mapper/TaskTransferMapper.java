package com.allinone.supply.mapper;

import org.apache.ibatis.annotations.Param;

public interface TaskTransferMapper {
    Long lockDocument(@Param("documentId") Long documentId);
    int countPermission(@Param("userId") Long userId, @Param("permission") String permission);
    int countRole(@Param("userId") Long userId, @Param("role") String role);
    int countScope(@Param("documentId") Long documentId, @Param("userId") Long userId);
    int countConflict(@Param("documentId") Long documentId, @Param("userId") Long userId);
}
