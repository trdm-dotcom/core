package com.homer.core.common.utils.validator;

import com.homer.core.common.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CombineValidator extends Validator {
    private List<Pair<Validator, Consumer>> validators;

    public CombineValidator() {
        super(null, null);
        this.validators = new ArrayList<>();
    }

    public CombineValidator add(Validator validator) {
        return add(validator, null);
    }

    public CombineValidator add(Validator validator, Consumer consumer) {
        validator.setException(this.exception);
        validator.notThrowOnFail();
        this.validators.add(new Pair<>(validator, consumer));
        return this;
    }

    @Override
    protected Object doCheck() {
        this.validators.forEach(p -> {
            Object result = p.getLeft().check();
            if (p.getRight() != null) {
                p.getRight().accept(result);
            }
            if (p.getLeft().isError()) {
                this.addError(p.getLeft().getException().getErrors().get(0));
            }
        });
        if (this.isError()) {
            doThrow();
        }
        return null;
    }
}
