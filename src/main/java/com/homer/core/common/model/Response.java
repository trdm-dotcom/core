package com.homer.core.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.exceptions.SubErrorsException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements DefaultPartitionBody {
    private T data;
    private Status status;

    public Response(T data) {
        this.data = data;
    }

    public Response(Status status) {
        this.status = status;
    }

    @JsonIgnore
    @Override
    public String getMessageKey() {
        if (data != null && data instanceof Body) {
            return ((Body) data).getMessageKey();
        }
        return null;
    }

    public static Response<?> fromException(Throwable e) {
        if (e instanceof GeneralException) {
            return fromException((GeneralException) e);
        }
        return fromException(new GeneralException().source(e));
    }

    public static Response<?> fromException(GeneralException e) {
        if (e instanceof SubErrorsException) {
            return new Response<>(from((SubErrorsException) e));
        }
        return new Response<>(from(e));
    }

    public static Status from(GeneralException e) {
        return new Status(
                e.getCode(),
                e.getMessageParams()
        );
    }

    public static Status from(SubErrorsException e) {
        Status status = new Status(
                e.getCode(),
                e.getMessageParams()
        );
        e.getErrors().forEach(err -> status.add(
                new Error(
                        err.getCode(),
                        err.getParam(),
                        err.getMessageParams()
                )
        ));
        return status;
    }

    /**
     * this method return an empty success response.
     *
     * @return
     */
    public static Response<HashMap<String, String>> empty() {
        return new Response<>(new HashMap<>());
    }
}
