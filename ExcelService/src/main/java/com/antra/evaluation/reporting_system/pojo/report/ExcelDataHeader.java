package com.antra.evaluation.reporting_system.pojo.report;

import lombok.Data;

@Data
public class ExcelDataHeader {
    private String name;

    private ExcelDataType type;

    private int width;
}
