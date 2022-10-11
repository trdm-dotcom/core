package com.homer.core.common.exceptions;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class SubErrorsException extends GeneralException {
    private List<FieldError> errors = new ArrayList<>();

    public SubErrorsException add(FieldError error) {
        this.errors.add(error);
        return this;
    }

    public SubErrorsException add(String code, String param, List<String> messageParams) {
        this.errors.add(new FieldError(code, param, messageParams));
        return this;
    }

    public SubErrorsException(String code) {
        super(code);
    }

    public SubErrorsException(String code, List<String> messageParams) {
        super(code, messageParams);
    }
}
