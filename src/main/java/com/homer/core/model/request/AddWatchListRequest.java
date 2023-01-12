package com.homer.core.model.request;

import com.homer.core.common.model.DataRequest;
import com.homer.core.common.utils.validator.CombineValidator;
import com.homer.core.common.utils.validator.NumberValidator;
import com.homer.core.common.utils.validator.StringValidator;
import lombok.Data;

@Data
public class AddWatchListRequest extends DataRequest {
    private Long postId;
    private String hash;

    public void validate() {
        new CombineValidator()
                .add(new StringValidator("hash", this.hash).empty())
                .add(new NumberValidator("postId", this.postId)).check();
    }
}
