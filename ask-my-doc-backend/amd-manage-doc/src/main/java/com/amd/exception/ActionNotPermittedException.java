package com.amd.exception;

import lombok.Getter;

@Getter
public class ActionNotPermittedException extends RuntimeException{

    private final String username;
    private final String resourceName;
    private final Object resource;

    public ActionNotPermittedException(String username, String resourceName, Object resource) {
        super(String.format("%s not allowed to access %s: '%s'", username, resourceName, resource));
        this.username = username;
        this.resourceName = resourceName;
        this.resource = resource;
    }
}
