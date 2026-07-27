package com.amd.dto;

import com.amd.enums.AiProvider;
import lombok.Data;

@Data
public class AiRequest {
    private AiProvider aiProvider;
    private String modelName;
}
