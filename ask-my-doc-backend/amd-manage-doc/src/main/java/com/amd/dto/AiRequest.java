package com.amd.dto;

import com.amd.config.AiProvider;
import lombok.Data;

@Data
public class AiRequest {
    private AiProvider aiProvider;
    private String modelName;
}
