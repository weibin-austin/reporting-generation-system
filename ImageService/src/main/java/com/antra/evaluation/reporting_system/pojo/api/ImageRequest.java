package com.antra.evaluation.reporting_system.pojo.api;

import lombok.Data;

import java.util.List;

@Data
public class ImageRequest {

    private String reqId;

    private List<String> headers;

    private String description;

    private List<List<String>> data;

    private String submitter;
}
