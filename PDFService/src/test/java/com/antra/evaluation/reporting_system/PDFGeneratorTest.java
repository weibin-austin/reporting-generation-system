package com.antra.evaluation.reporting_system;

import com.antra.evaluation.reporting_system.pojo.api.PDFRequest;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import com.antra.evaluation.reporting_system.service.PDFGenerator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PDFGeneratorTest {

    @Test
    public void generatesValidPdfFromTemplate() throws IOException {
        PDFRequest request = new PDFRequest();
        request.setDescription("Math Report");
        request.setSubmitter("AustinTest");
        request.setHeaders(List.of("Name", "Score"));
        request.setData(List.of(List.of("Alice", "100"), List.of("Bob", "97")));

        PDFFile generated = new PDFGenerator().generate(request);

        File file = new File(generated.getFileLocation());
        try {
            assertTrue(file.exists());
            assertTrue(generated.getFileSize() > 0);
            byte[] header = new byte[4];
            try (FileInputStream fis = new FileInputStream(file)) {
                assertEquals(4, fis.read(header));
            }
            assertEquals("%PDF", new String(header, StandardCharsets.US_ASCII));
        } finally {
            file.delete();
        }
    }
}
