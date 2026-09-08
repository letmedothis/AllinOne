package com.allinone.common.approval;

/** 各业务模块实现校验与原子生效；调用者提供事务。 */
public interface ReviewHandler {
    String type();
    void prepare(ReviewCase request, boolean creating);
    void validateSubmission(ReviewCase request);
    void apply(ReviewCase request);
    default void submitted(ReviewCase request) { }
    default void cancelled(ReviewCase request) { }
}
