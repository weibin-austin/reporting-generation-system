package com.antra.evaluation.reporting_system.pojo.report;

import lombok.Data;

import java.util.List;

@Data
public class ExcelData {

    private String title;

    private String submitter;

    private String fileId;

    private List<ExcelDataSheet> sheets;
}

