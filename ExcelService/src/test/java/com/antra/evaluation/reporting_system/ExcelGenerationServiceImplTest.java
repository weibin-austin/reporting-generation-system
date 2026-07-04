package com.antra.evaluation.reporting_system;

import com.antra.evaluation.reporting_system.pojo.report.ExcelData;
import com.antra.evaluation.reporting_system.pojo.report.ExcelDataHeader;
import com.antra.evaluation.reporting_system.pojo.report.ExcelDataSheet;
import com.antra.evaluation.reporting_system.service.ExcelGenerationServiceImpl;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExcelGenerationServiceImplTest {

    private final ExcelGenerationServiceImpl service = new ExcelGenerationServiceImpl();

    private ExcelData sampleData() {
        ExcelData data = new ExcelData();
        data.setTitle("Test Report");
        data.setFileId("test-" + UUID.randomUUID());
        ExcelDataSheet sheet = new ExcelDataSheet();
        sheet.setTitle("sheet-1");
        sheet.setHeaders(List.of(new ExcelDataHeader("Name"), new ExcelDataHeader("Score")));
        List<List<Object>> rows = new ArrayList<>();
        rows.add(new ArrayList<>(List.of("Alice", "100")));
        rows.add(new ArrayList<>(List.of("Bob", "97")));
        sheet.setDataRows(rows);
        data.setSheets(new ArrayList<>(List.of(sheet)));
        return data;
    }

    @Test
    public void generatesWorkbookWithHeadersAndRows() throws IOException {
        File file = service.generateExcelReport(sampleData());
        try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(file))) {
            Sheet sheet = workbook.getSheet("sheet-1");
            assertNotNull(sheet);
            assertEquals("Name", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Score", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("Alice", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("97", sheet.getRow(2).getCell(1).getStringCellValue());
        } finally {
            assertTrue(file.delete());
        }
    }

    @Test
    public void rejectsDataWithoutSheets() {
        ExcelData data = sampleData();
        data.setSheets(new ArrayList<>());
        assertThrows(RuntimeException.class, () -> service.generateExcelReport(data));
    }

    @Test
    public void rejectsSheetWithoutTitle() {
        ExcelData data = sampleData();
        data.getSheets().get(0).setTitle("");
        assertThrows(RuntimeException.class, () -> service.generateExcelReport(data));
    }

    @Test
    public void rejectsRowWiderThanHeaders() {
        ExcelData data = sampleData();
        data.getSheets().get(0).getDataRows().add(new ArrayList<>(List.of("Carol", "89", "extra")));
        assertThrows(RuntimeException.class, () -> service.generateExcelReport(data));
    }
}
