package com.ayd.security;

import lombok.Data;

import java.util.List;

@Data
public class ResourceRepresentation {
    public String name;
    public List<String> uris;
}
