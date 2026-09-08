package com.allinone.supply.support;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.supply.mapper.TaskTransferMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 转交管理权与接替人的业务资格分别校验；管理员也不能指定不合格审批人。 */
@Component
public class TaskTransferAuthorization {
    @Autowired private TaskTransferMapper mapper;

    public void requireOperator(Long documentId, String permission) {
        Long actor = SecurityUtils.getUserId();
        if (!SecurityUtils.isAdmin() && (mapper.countPermission(actor, permission) == 0
                || !isSupervisor(actor) || mapper.countScope(documentId, actor) == 0)) {
            throw new ServiceException("无权转交该单据：需要有效主管角色、转交权限及单据数据范围");
        }
    }

    public void requireRecipient(Long documentId, Long userId, String permission) {
        if (userId == null || mapper.countPermission(userId, permission) == 0) {
            throw new ServiceException("接替人不存在、已停用或没有节点办理权限");
        }
        if (mapper.countConflict(documentId, userId) > 0) {
            throw new ServiceException("接替人不能是申请人或本轮已办理其他审批节点的人员");
        }
    }

    public boolean isSupervisor(Long userId) {
        return mapper.countRole(userId, "supervisor") > 0
                || mapper.countRole(userId, "purchasing_supervisor") > 0
                || mapper.countRole(userId, "purchase_supervisor") > 0;
    }

    public boolean hasRole(Long userId, String role) { return mapper.countRole(userId, role) > 0; }

    public void lockDocument(Long documentId) {
        if (mapper.lockDocument(documentId) == null) throw new ServiceException("单据已变化，请刷新后重试");
    }
}
