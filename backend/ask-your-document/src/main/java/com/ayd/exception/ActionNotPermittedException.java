package com.ayd.exception;

import lombok.Getter;

@Getter
public class ActionNotPermittedException extends RuntimeException{

    private final String userId;
    private final String resourceName;
    private final Object resource;

    public ActionNotPermittedException(String userId, String resourceName, Object resource) {
        super(String.format("%s not allowed to access %s: '%s'", userId, resourceName, resource));
        this.userId = userId;
        this.resourceName = resourceName;
        this.resource = resource;
    }
}
