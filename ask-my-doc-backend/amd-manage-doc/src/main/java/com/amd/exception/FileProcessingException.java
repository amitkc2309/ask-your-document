package com.amd.exception;

import lombok.Getter;

/**
 * Exception thrown when there's an error processing a file.
 */
@Getter
public class FileProcessingException extends RuntimeException {
    
    private final String fileName;
    private final String operation;
    
    public FileProcessingException(String fileName, String operation, String message) {
        super(String.format("Error %s file '%s': %s", operation, fileName, message));
        this.fileName = fileName;
        this.operation = operation;
    }
    
    public FileProcessingException(String fileName, String operation, Throwable cause) {
        super(String.format("Error %s file '%s': %s", operation, fileName, cause.getMessage()), cause);
        this.fileName = fileName;
        this.operation = operation;
    }
}