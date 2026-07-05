package com.antra.evaluation.reporting_system.pojo.report;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@Entity(name = "excel_file")
public class ExcelFile {

    @Id
    private String fileId;

    private String fileName;

    private String fileLocation;

    private String submitter;

    private Long fileSize;

    private String description;

    private LocalDateTime generatedTime;
}
