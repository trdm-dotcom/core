package com.homer.core.common.exceptions;


import com.homer.core.common.constants.ErrorCodeEnums;

public class UriNotFoundException extends GeneralException {
    public UriNotFoundException() {
        super(ErrorCodeEnums.URI_NOT_FOUND.name());
    }
}
