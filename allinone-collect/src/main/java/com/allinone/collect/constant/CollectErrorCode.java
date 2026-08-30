package com.allinone.collect.constant;

/**
 * 填报模块业务错误码（总体设计 §7.2 错误码体系），
 * 经 ServiceException 携带并由 GlobalExceptionHandler 以 AjaxResult.code 返回前端。
 */
public final class CollectErrorCode {

    /** 填报模板不存在 */
    public static final int TEMPLATE_NOT_FOUND = 1001;

    /** 模板未发布不可用 */
    public static final int TEMPLATE_NOT_PUBLISHED = 1002;

    /** 数据状态不允许修改 */
    public static final int DATA_STATUS_NOT_EDITABLE = 1003;

    /** 数据版本冲突（乐观锁） */
    public static final int DATA_VERSION_CONFLICT = 1004;

    private CollectErrorCode() {
    }
}
