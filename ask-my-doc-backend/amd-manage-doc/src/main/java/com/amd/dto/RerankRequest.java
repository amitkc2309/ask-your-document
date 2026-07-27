package com.amd.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RerankRequest {
    private String query;
    private List<RerankRequestItem> documents;
}
