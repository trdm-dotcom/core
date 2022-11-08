package com.homer.core.model.request;

import com.homer.core.common.model.DataRequest;
import com.homer.core.common.utils.validator.CombineValidator;
import com.homer.core.common.utils.validator.NumberValidator;
import com.homer.core.common.utils.validator.StringValidator;
import com.homer.core.utils.InputValidation;
import lombok.Data;

@Data
public class RepairerDepositUrlRequest extends DataRequest {
    private String orderInfo;
    private String bankCode;
    private Double amount;
    private String hash;

    public void validate(){
        new CombineValidator()
                .add(new StringValidator("orderInfo", InputValidation.removeAccent(this.orderInfo)).empty())
                .add(new StringValidator("bankCode", this.bankCode).empty())
                .add(new StringValidator("hash", this.hash).empty())
                .add(new NumberValidator("amount", this.amount).min(0.0)).check();
    }
}
