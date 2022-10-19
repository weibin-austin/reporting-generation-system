package com.antra.evaluation.reporting_system.pojo.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExcelFile {

    private String fileId;

    private String fileName;

    private String fileLocation;

    private String submitter;

    private Long fileSize;

    private String description;

    private LocalDateTime generatedTime;
}
