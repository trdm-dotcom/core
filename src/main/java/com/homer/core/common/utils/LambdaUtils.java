package com.homer.core.common.utils;

import java.util.function.BiConsumer;

public class LambdaUtils {
    public interface BiConsumerException<T, R, E extends Exception> {
        void accept(T t, R r) throws E;
    }

    public static <T, R, E extends Exception> BiConsumer<T, R> throwBiConsumer(BiConsumerException<T, R, E> biConsumer) {
        return (t, r) -> {
            try {
                biConsumer.accept(t, r);
            } catch (Exception e) {
                throwActualException(e);
            }
        };
    }

    private static <E extends Exception> void throwActualException(Exception exception) throws E {
        throw (E) exception;
    }
}
