package com.antra.evaluation.reporting_system.pojo.api;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class ErrorResponse {

    private String message;

    private HttpStatus status;
}
