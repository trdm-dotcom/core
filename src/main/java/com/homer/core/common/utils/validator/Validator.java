package com.homer.core.common.utils.validator;


import com.homer.core.common.exceptions.FieldError;
import com.homer.core.common.exceptions.InvalidParameterException;
import lombok.Data;
import rx.Observer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Data
public abstract class Validator<T> {
    protected Validator checkIfPassed;
    protected InvalidParameterException exception;
    protected String fieldName;
    protected T fieldValue;
    protected boolean throwOnFail = true;
    protected Observer<?> observer;
    protected Consumer<Object> consumer;

    public Validator consume(Consumer<Object> consumer) {
        this.consumer = consumer;
        return this;
    }

    public Validator notThrowOnFail() {
        this.throwOnFail = false;
        return this;
    }

    public Validator(String fieldName, T fieldValue) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public boolean isError() {
        return this.exception != null && !this.exception.getErrors().isEmpty();
    }

    public void doThrow() {
        if (!this.isError()) {
            return;
        }
        throw this.exception;
    }

    public Object addError(FieldError error) {
        this.exception.add(error);
        return null;
    }

    public Object addError(String code, String param) {
        this.exception.add(new FieldError(code, param));
        return null;
    }

    public Object addError(String code, String param, List<String> messageParams) {
        this.exception.add(new FieldError(code, param, messageParams));
        return null;
    }

    public Object addError(String code, String param, String... messageParams) {
        this.exception.add(new FieldError(code, param, Arrays.asList(messageParams)));
        return null;
    }

    public void dependOn(Validator other) {
        this.checkIfPassed = other;
    }

    public Object check() {
        this.exception = new InvalidParameterException();
        if (this.checkIfPassed != null && this.checkIfPassed.check() == null) {
            return null;
        }

        Object transformed = this.doCheck();
        if (this.throwOnFail) {
            this.doThrow();
        }
        if (!this.isError() && this.consumer != null && transformed != null) {
            this.consumer.accept(transformed);
        }
        return transformed;
    }

    protected abstract Object doCheck();
}
