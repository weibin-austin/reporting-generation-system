package com.antra.evaluation.reporting_system.pojo.report;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExcelDataHeader {
    private String name;

    private ExcelDataType type;

    private int width;

    public ExcelDataHeader(String name) {
        this.name = name;
    }
}
