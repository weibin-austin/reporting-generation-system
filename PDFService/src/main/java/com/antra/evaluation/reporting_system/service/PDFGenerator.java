package com.antra.evaluation.reporting_system.service;

import com.antra.evaluation.reporting_system.pojo.api.PDFRequest;
import com.antra.evaluation.reporting_system.pojo.exception.PDFGenerationException;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PDFGenerator {
    private static final Logger log = LoggerFactory.getLogger(PDFGenerator.class);

    public PDFFile generate(PDFRequest request) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("desc_str", request.getDescription());
        StringBuilder data = new StringBuilder();
        for (List<String> datum : request.getData()) {
            data.append(String.join(", ", datum));
            data.append("\r\n");
        }
        parameters.put("content_str", String.join(",", request.getHeaders()) + "\r\n" + data.toString());

        List<Object> itemList = List.of("Empty");
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(itemList);

        // Load the template as a stream: ResourceUtils.getFile() cannot open
        // classpath resources when running from a packaged jar.
        try (InputStream jaspStream = new ClassPathResource("Coffee_Landscape.jasper").getInputStream()) {
            JasperPrint jprint = JasperFillManager.fillReport(jaspStream, parameters, dataSource);
            File temp = File.createTempFile(request.getSubmitter(),"_tmp.pdf");
            JasperExportManager.exportReportToPdfFile(jprint, temp.getAbsolutePath());
            PDFFile generatedFile = new PDFFile();
            generatedFile.setFileLocation(temp.getAbsolutePath());
            generatedFile.setFileName(temp.getName());
            generatedFile.setFileSize(temp.length());
            log.info("Generated PDF file: {}", generatedFile);
            return generatedFile;
        } catch (IOException | JRException e) {
            log.error("Error in generating PDF file",e);
            throw new PDFGenerationException();
        }
    }
}
