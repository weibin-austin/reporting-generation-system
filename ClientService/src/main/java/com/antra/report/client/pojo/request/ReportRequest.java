package com.antra.report.client.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Null;
import java.util.List;

@Data
public class ReportRequest {
    @Null // this field will be set in the service. shouldn't be passed from client
    private String reqId;
    @NotEmpty
    private List<String> headers;
    @NotBlank
    private String description;
    @NotEmpty
    private List<List<String>> data;
    @NotBlank
    private String submitter;
}
