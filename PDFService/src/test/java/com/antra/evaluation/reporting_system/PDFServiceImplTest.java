package com.antra.evaluation.reporting_system;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.evaluation.reporting_system.pojo.api.PDFRequest;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import com.antra.evaluation.reporting_system.repo.PDFRepository;
import com.antra.evaluation.reporting_system.service.PDFGenerator;
import com.antra.evaluation.reporting_system.service.PDFServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PDFServiceImplTest {

    @Mock
    PDFRepository repository;
    @Mock
    PDFGenerator generator;
    @Mock
    AmazonS3 s3Client;

    @Test
    public void createPdfUploadsToS3AndCleansUpTempFile() throws IOException {
        PDFServiceImpl service = new PDFServiceImpl(repository, generator, s3Client);
        ReflectionTestUtils.setField(service, "s3Bucket", "test-bucket");

        File temp = File.createTempFile("pdf-service-test", ".pdf");
        PDFFile generated = new PDFFile();
        generated.setFileLocation(temp.getAbsolutePath());
        generated.setFileName(temp.getName());
        generated.setFileSize(temp.length());
        when(generator.generate(any())).thenReturn(generated);

        PDFRequest request = new PDFRequest();
        request.setDescription("Math Report");
        request.setSubmitter("Austin");
        request.setHeaders(List.of("Name"));
        request.setData(List.of(List.of("Alice")));

        PDFFile result = service.createPDF(request);

        assertNotNull(result.getId());
        assertEquals("Austin", result.getSubmitter());
        assertEquals("test-bucket/" + result.getId(), result.getFileLocation());
        verify(s3Client).putObject(eq("test-bucket"), eq(result.getId()), any(File.class));
        verify(repository).save(result);
        assertFalse(temp.exists());
    }
}
