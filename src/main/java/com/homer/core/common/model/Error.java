package com.homer.core.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Error {
    private String code;
    private String param;
    private List<String> messageParams;
}
