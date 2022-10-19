package com.antra.evaluation.reporting_system.pojo.report;


import lombok.Data;

import java.util.List;

@Data
public class ExcelDataSheet {

    private String title;

    private List<ExcelDataHeader> headers;

    private List<List<Object>> dataRows;
}
