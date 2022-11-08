package com.homer.core.common.utils.validator;

import com.homer.core.common.constants.ErrorCodeEnums;
import com.homer.core.common.utils.DefaultUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

import java.text.ParseException;

@Data
public class StringValidator extends Validator<String> {
    public static final IValidator<String> DATE_FORMAT_CHECK = s -> {
        try {
            return DefaultUtils.DATE_FORMAT().parse(s);
        } catch (ParseException e) {
            return null;
        }
    };
    public static final IValidator<String> EMAIL_FORMAT_CHECK = s
            -> EmailValidator.getInstance().isValid(s) ? s : null;
    public static final IValidator<String> PHONE_NUMBER = s -> s; // TODO later

    private boolean empty = false;
    private Object defaultValueIfEmpty;
    private IValidator<String> format;
    private IValidator<String> value;

    public StringValidator(String fieldName, String fieldValue) {
        super(fieldName, fieldValue);
    }

    public StringValidator empty(Object defaultValue) {
        this.defaultValueIfEmpty = defaultValue;
        return this.empty();
    }

    public StringValidator empty() {
        this.empty = true;
        return this;
    }

    public StringValidator format(IValidator validate) {
        this.format = validate;
        return this;
    }

    public StringValidator value(IValidator validate) {
        this.value = validate;
        return this;
    }

    private boolean passedEmpty() {
        return this.exception.getErrors().isEmpty() && !StringUtils.isEmpty(this.fieldValue);
    }

    protected Object doCheck() {
        Object result = null;
        if (this.empty && StringUtils.isEmpty(this.fieldValue)) {
            if (defaultValueIfEmpty != null) {
                return defaultValueIfEmpty;
            } else {
                this.addError(ErrorCodeEnums.EMPTY_VALUE.name(), this.fieldName);
                return null;
            }
        }
        if (this.format != null && this.passedEmpty()) {
            result = this.format.valid(this.fieldValue);
            if (result == null) {
                this.addError(ErrorCodeEnums.INVALID_FORMAT.name(), this.fieldName);
                return null;
            }
        }
        if (this.value != null && this.passedEmpty()) {
            result = this.value.valid(this.fieldValue);
            if (result == null) {
                this.addError(ErrorCodeEnums.INVALID_VALUE.name(), this.fieldName, this.fieldName, this.fieldValue);
                return null;
            }
        }
        return result;
    }
}
