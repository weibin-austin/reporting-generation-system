package com.antra.report.client.pojo.reponse;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExcelResponse {

    private String reqId;

    private String fileId;

    private String fileLocation;

    private long fileSize;

    private String fileName;

    private String submitter;

    private String description;

    private LocalDateTime generatedTime;

    private boolean failed;
}
