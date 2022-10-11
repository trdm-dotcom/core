package com.homer.core.common.exceptions;

import com.homer.core.common.constants.ErrorCodeEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralException extends RuntimeException {
    protected String code = ErrorCodeEnums.INTERNAL_SERVER_ERROR.name();
    protected List<String> messageParams;
    protected Throwable source;

    public GeneralException(String code, String... params) {
        this.code = code;
        this.messageParams = params == null ? null : Arrays.asList(params);
    }

    public GeneralException(String code, List<String> messageParams) {
        this.code = code;
        this.messageParams = messageParams;
    }

    @Override
    public String getMessage() {
        return this.code;
    }

    public GeneralException source(Throwable source) {
        this.source = source;
        return this;
    }

    @Override
    public synchronized Throwable getCause() {
        return this.source;
    }
}
