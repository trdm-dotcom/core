package com.homer.core.common.utils.validator;

import com.homer.core.common.constants.ErrorCodeEnums;

import java.util.Arrays;

public class NumberValidator extends Validator<Number> {
    private Double min;
    private Double max;
    private Double extract;
    private Number[] in;
    private boolean eq;
    private boolean notNull = true;

    public NumberValidator(String fieldName, Number fieldValue) {
        super(fieldName, fieldValue);
    }

    public NumberValidator eq() {
        this.eq = true;
        return this;
    }

    public NumberValidator notEmpty() {
        this.notNull = false;
        return this;
    }

    public NumberValidator in(Number... items) {
        in = items;
        return this;
    }

    public NumberValidator min(int min) {
        return this.min((double) min);
    }

    public NumberValidator min(long min) {
        return this.min((double) min);
    }

    public NumberValidator min(double min) {
        this.min = min;
        return this;
    }

    public NumberValidator max(int max) {
        return this.max((double) max);
    }

    public NumberValidator max(long max) {
        return this.max((double) max);
    }

    public NumberValidator max(double max) {
        this.max = max;
        return this;
    }

    public NumberValidator extract(int extract) {
        return this.extract((double) extract);
    }

    public NumberValidator extract(long extract) {
        return this.extract((double) extract);
    }

    public NumberValidator extract(double extract) {
        this.extract = extract;
        return this;
    }

    private boolean isEmpty() {
        return this.fieldValue == null;
    }

    @Override
    protected Object doCheck() {
        if (this.notNull && this.isEmpty()) {
            return this.addError(ErrorCodeEnums.EMPTY_VALUE.name(), this.fieldName);
        }
        if (!this.isEmpty() && this.extract != null && this.fieldValue.doubleValue() != this.extract) {
            return this.addError(ErrorCodeEnums.VALUE_MUST_EQUAL.name()
                    , this.fieldName, this.fieldName, String.valueOf(this.extract));
        }
        if (!this.isEmpty() && this.in != null) {
            boolean found = false;
            for (Number item : in) {
                if (item.equals(this.fieldValue)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return this.addError(ErrorCodeEnums.VALUE_MUST_IN.name()
                        , this.fieldName, this.fieldName, Arrays.toString(this.in));
            }
        }
        if (!this.isEmpty() && this.min != null) {
            if (this.eq && this.fieldValue.doubleValue() < this.min) {
                return this.addError(ErrorCodeEnums.VALUE_MUST_LESS.name()
                        , this.fieldName, this.fieldName, String.valueOf(this.min));
            }
            if (!this.eq && this.fieldValue.doubleValue() <= this.min) {
                return this.addError(ErrorCodeEnums.VALUE_MUST_LESS_OR_EQUAL.name()
                        , this.fieldName, this.fieldName, String.valueOf(this.min));
            }
        }
        if (!this.isEmpty() && this.max != null) {
            if (this.eq && this.fieldValue.doubleValue() > this.max) {
                return this.addError(ErrorCodeEnums.VALUE_MUST_GREATER.name()
                        , this.fieldName, this.fieldName, String.valueOf(this.min));
            }
            if (!this.eq && this.fieldValue.doubleValue() >= this.max) {
                return this.addError(ErrorCodeEnums.VALUE_MUST_GREATER_OR_EQUAL.name()
                        , this.fieldName, this.fieldName, String.valueOf(this.min));
            }
        }
        return null;
    }
}
