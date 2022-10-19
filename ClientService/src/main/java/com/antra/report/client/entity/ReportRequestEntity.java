package com.antra.report.client.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity(name="report_request")
public class ReportRequestEntity {
    @Id
    private String reqId;
    private String submitter;
    private String description;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.EAGER) // default is already eager here
    @JoinColumn(name="pdf_report_id")
    private PDFReportEntity pdfReport;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.EAGER) // default is already eager here
    @JoinColumn(name="excel_report_id")
    private ExcelReportEntity excelReport;
}
