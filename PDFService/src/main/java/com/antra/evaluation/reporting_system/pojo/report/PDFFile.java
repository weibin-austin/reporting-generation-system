package com.antra.evaluation.reporting_system.pojo.report;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
@Data
public class PDFFile {

    private String id;

    private String fileName;

    private String fileLocation;

    private String submitter;

    private Long fileSize;

    private String description;

    private LocalDateTime generatedTime;
}
