package com.antra.evaluation.reporting_system.pojo.api;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class ExcelRequest {
    private String reqId;

    @NotEmpty
    private List<String> headers;

    private String description;

    private List<List<String>> data;

    private String submitter;
}
