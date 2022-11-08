package com.homer.core.common.exceptions;

import com.homer.core.common.constants.ErrorCodeEnums;
import lombok.Data;

@Data
public class InvalidParameterException extends SubErrorsException {
    public InvalidParameterException() {
        super(ErrorCodeEnums.INVALID_PARAMETER.name());
    }
}
