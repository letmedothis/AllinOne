package com.allinone.supply.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.util.Date;

/** 财务记录已在线下完成的支付事实，不调用银行执行转账。 */
public class PaymentExecution {
    @NotNull private Integer revision;
    @NotNull @PastOrPresent @JsonFormat(pattern = "yyyy-MM-dd") private Date paidAt;
    @NotBlank @Size(max = 120) private String reference;
    @NotBlank @Size(max = 120) private String payerAccount;
    public Integer getRevision() { return revision; } public void setRevision(Integer v) { revision = v; }
    public Date getPaidAt() { return paidAt; } public void setPaidAt(Date v) { paidAt = v; }
    public String getReference() { return reference; } public void setReference(String v) { reference = v; }
    public String getPayerAccount() { return payerAccount; } public void setPayerAccount(String v) { payerAccount = v; }
}
