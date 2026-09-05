package com.ayd.dto;

import com.ayd.enums.AiProvider;
import lombok.Data;

@Data
public class AiRequest {
    private AiProvider aiProvider;
    private String modelName;
}
