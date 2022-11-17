package com.homer.core.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.homer.core.common.utils.validator.CombineValidator;
import com.homer.core.common.utils.validator.NumberValidator;
import com.homer.core.common.utils.validator.StringValidator;
import lombok.Data;

@Data
public class VnPayRequest {
    @JsonProperty("vnp_SecureHash")
    private String vnpSecureHash;

    @JsonProperty("vnp_TmnCode")
    private String vnpTmnCode;

    @JsonProperty("vnp_TxnRef")
    private String vnpTxnRef;

    @JsonProperty("vnp_Amount")
    private Double vnpAmount;

    @JsonProperty("vnp_OrderInfo")
    private String vnpOrderInfo;

    @JsonProperty("vnp_ResponseCode")
    private String vnpResponseCode;

    @JsonProperty("vnp_BankCode")
    private String vnpBankCode;

    @JsonProperty("vnp_BankTranNo")
    private String vnpBankTranNo;

    @JsonProperty("vnp_CardType")
    private String vnpCardType;

    @JsonProperty("vnp_PayDate")
    private String vnpPayDate;

    @JsonProperty("vnp_TransactionNo")
    private String vnpTransactionNo;

    @JsonProperty("vnp_TransactionStatus")
    private String vnpTransactionStatus;

    public void validate(){
        new CombineValidator()
                .add(new StringValidator("vnp_SecureHash", vnpSecureHash).empty())
                .add(new StringValidator("vnp_TxnRef", this.vnpTxnRef).empty())
                .add(new NumberValidator("vnp_Amount", this.vnpAmount))
                .add(new StringValidator("vnp_ResponseCode", this.vnpResponseCode).empty()).check();
    }
}
