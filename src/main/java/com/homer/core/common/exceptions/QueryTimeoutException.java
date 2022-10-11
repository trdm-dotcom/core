package com.homer.core.common.exceptions;

public class QueryTimeoutException extends GeneralException {
    public QueryTimeoutException() {
        super("QUERY_TIMEOUT");
    }
}
