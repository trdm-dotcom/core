package com.homer.core.common.utils.validator;

import com.homer.core.common.constants.ErrorCodeEnums;
import org.apache.commons.lang3.EnumUtils;

public class EnumValidator<T> extends Validator<String> {
    public EnumValidator(String fieldName, String fieldValue, Class<T> enumClass) {
        super(fieldName, fieldValue);
        this.enumClass = enumClass;
    }

    private Class enumClass;
    private boolean valid = false;

    public EnumValidator validate() {
        this.valid = true;
        return this;
    }

    @Override
    protected Object doCheck() {
        Object result = null;
        if (valid && !EnumUtils.isValidEnum(enumClass, fieldValue)) {
            return this.addError(ErrorCodeEnums.INVALID_VALUE.name(), this.fieldName);
        }
        return result;
    }
}
