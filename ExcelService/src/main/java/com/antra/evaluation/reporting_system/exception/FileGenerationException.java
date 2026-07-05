package com.antra.evaluation.reporting_system.exception;

public class FileGenerationException extends RuntimeException{
    public FileGenerationException(Throwable cause) {
        super(cause);
    }

    public FileGenerationException(String message) {
        super(message);
    }
}
