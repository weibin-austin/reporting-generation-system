package com.antra.evaluation.reporting_system.pojo.api;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExcelResponse {

    private String reqId;

    private String fileId;

    private String fileLocation;

    private String fileDownloadLink;

    private Long fileSize;

    private String fileName;

    private String submitter;

    private String description;

    private LocalDateTime generatedTime;

    private boolean failed;
}
