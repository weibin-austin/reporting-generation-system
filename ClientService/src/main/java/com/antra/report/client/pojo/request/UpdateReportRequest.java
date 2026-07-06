package com.antra.report.client.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateReportRequest {
    @NotBlank
    private String description;
}
