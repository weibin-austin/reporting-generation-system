package com.antra.evaluation.reporting_system.pojo.api;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class PDFResponse {

    private String fileId;

    private String reqId;

    private String fileLocation;

    private long fileSize;

    private boolean failed;

}
