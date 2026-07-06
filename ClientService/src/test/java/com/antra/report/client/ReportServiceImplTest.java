package com.antra.report.client;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.report.client.entity.ReportRequestEntity;
import com.antra.report.client.entity.ReportStatus;
import com.antra.report.client.exception.RequestNotFoundException;
import com.antra.report.client.pojo.EmailType;
import com.antra.report.client.pojo.reponse.ExcelResponse;
import com.antra.report.client.pojo.reponse.ImageResponse;
import com.antra.report.client.pojo.reponse.PDFResponse;
import com.antra.report.client.pojo.reponse.SqsResponse;
import com.antra.report.client.pojo.request.ReportRequest;
import com.antra.report.client.repository.ReportRequestRepo;
import com.antra.report.client.service.EmailService;
import com.antra.report.client.service.ReportServiceImpl;
import com.antra.report.client.service.SNSService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReportServiceImplTest {

    private static final String EXCEL_URL = "http://excel-service/excel";
    private static final String PDF_URL = "http://pdf-service/pdf";
    private static final String IMAGE_URL = "http://image-service/image";
    private static final String RECIPIENT = "austin.sun89@gmail.com";

    @Mock
    ReportRequestRepo reportRequestRepo;
    @Mock
    SNSService snsService;
    @Mock
    AmazonS3 s3Client;
    @Mock
    EmailService emailService;
    @Mock
    RestTemplate restTemplate;

    private ReportServiceImpl service;
    private final Map<String, ReportRequestEntity> store = new HashMap<>();

    @BeforeEach
    public void setUp() {
        service = new ReportServiceImpl(reportRequestRepo, snsService, s3Client, emailService, restTemplate);
        ReflectionTestUtils.setField(service, "notificationRecipient", RECIPIENT);
        ReflectionTestUtils.setField(service, "excelServiceUrl", EXCEL_URL);
        ReflectionTestUtils.setField(service, "pdfServiceUrl", PDF_URL);
        ReflectionTestUtils.setField(service, "imageServiceUrl", IMAGE_URL);
        // back the mocked repository with a map so saves are visible to later reads
        lenient().when(reportRequestRepo.save(any(ReportRequestEntity.class))).thenAnswer(inv -> {
            ReportRequestEntity entity = inv.getArgument(0);
            store.put(entity.getReqId(), entity);
            return entity;
        });
        lenient().when(reportRequestRepo.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.<String>getArgument(0))));
    }

    private ReportRequest sampleRequest() {
        ReportRequest request = new ReportRequest();
        request.setDescription("Math Report");
        request.setSubmitter("Austin");
        request.setHeaders(List.of("Name", "Score"));
        request.setData(List.of(List.of("Alice", "100")));
        return request;
    }

    private void stubExcelSuccess() {
        when(restTemplate.postForEntity(eq(EXCEL_URL), any(), eq(ExcelResponse.class))).thenAnswer(inv -> {
            ReportRequest req = inv.getArgument(1);
            ExcelResponse response = new ExcelResponse();
            response.setReqId(req.getReqId());
            response.setFileId("excel-file-1");
            response.setFileLocation("/tmp/excel-file-1.xlsx");
            response.setFileSize(100L);
            return ResponseEntity.ok(response);
        });
    }

    private void stubPdfSuccess() {
        when(restTemplate.postForEntity(eq(PDF_URL), any(), eq(PDFResponse.class))).thenAnswer(inv -> {
            ReportRequest req = inv.getArgument(1);
            PDFResponse response = new PDFResponse();
            response.setReqId(req.getReqId());
            response.setFileId("pdf-file-1");
            response.setFileLocation("bucket/pdf-file-1");
            response.setFileSize(200L);
            return ResponseEntity.ok(response);
        });
    }

    private void stubImageSuccess() {
        when(restTemplate.postForEntity(eq(IMAGE_URL), any(), eq(ImageResponse.class))).thenAnswer(inv -> {
            ReportRequest req = inv.getArgument(1);
            ImageResponse response = new ImageResponse();
            response.setReqId(req.getReqId());
            response.setFileId("image-file-1");
            response.setFileLocation("bucket/image-file-1");
            response.setFileSize(300L);
            return ResponseEntity.ok(response);
        });
    }

    private SqsResponse sqs(String reqId, String fileId) {
        SqsResponse r = new SqsResponse();
        r.setReqId(reqId);
        r.setFileId(fileId);
        return r;
    }

    @Test
    public void syncGenerationCallsAllThreeServicesAndSendsOneSuccessEmail() {
        stubExcelSuccess();
        stubPdfSuccess();
        stubImageSuccess();
        ReportRequest request = sampleRequest();

        service.generateReportsSync(request);

        ReportRequestEntity entity = store.get(request.getReqId());
        assertNotNull(entity);
        assertEquals(ReportStatus.COMPLETED, entity.getExcelReport().getStatus());
        assertEquals(ReportStatus.COMPLETED, entity.getPdfReport().getStatus());
        assertEquals(ReportStatus.COMPLETED, entity.getImageReport().getStatus());
        assertEquals("image-file-1", entity.getImageReport().getFileId());
        verify(restTemplate).postForEntity(eq(EXCEL_URL), any(), eq(ExcelResponse.class));
        verify(restTemplate).postForEntity(eq(PDF_URL), any(), eq(PDFResponse.class));
        verify(restTemplate).postForEntity(eq(IMAGE_URL), any(), eq(ImageResponse.class));
        verify(emailService, times(1)).sendEmail(eq(RECIPIENT), eq(EmailType.SUCCESS), eq("Austin"));
    }

    @Test
    public void syncPdfFailureMarksLegFailedAndSendsFailureEmail() {
        stubExcelSuccess();
        stubImageSuccess();
        when(restTemplate.postForEntity(eq(PDF_URL), any(), eq(PDFResponse.class)))
                .thenThrow(new RestClientException("pdf service down"));
        ReportRequest request = sampleRequest();

        service.generateReportsSync(request);

        ReportRequestEntity entity = store.get(request.getReqId());
        assertEquals(ReportStatus.COMPLETED, entity.getExcelReport().getStatus());
        assertEquals(ReportStatus.COMPLETED, entity.getImageReport().getStatus());
        assertEquals(ReportStatus.FAILED, entity.getPdfReport().getStatus());
        verify(emailService, times(1)).sendEmail(eq(RECIPIENT), eq(EmailType.FAILURE), eq("Austin"));
        verify(emailService, never()).sendEmail(any(), eq(EmailType.SUCCESS), any());
    }

    @Test
    public void asyncGenerationPublishesToSnsAndDefersEmail() {
        ReportRequest request = sampleRequest();

        service.generateReportsAsync(request);

        verify(snsService).sendReportNotification(request);
        ReportRequestEntity entity = store.get(request.getReqId());
        assertEquals(ReportStatus.PENDING, entity.getExcelReport().getStatus());
        assertEquals(ReportStatus.PENDING, entity.getPdfReport().getStatus());
        assertEquals(ReportStatus.PENDING, entity.getImageReport().getStatus());
        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    public void emailIsSentOnceAndOnlyAfterAllThreeLegsComplete() {
        ReportRequest request = sampleRequest();
        service.generateReportsAsync(request);
        String reqId = request.getReqId();

        service.updateAsyncExcelReport(sqs(reqId, "excel-file-1"));
        service.updateAsyncPDFReport(sqs(reqId, "pdf-file-1"));
        // two of three legs done -> still no email
        verify(emailService, never()).sendEmail(any(), any(), any());

        service.updateAsyncImageReport(sqs(reqId, "image-file-1"));
        verify(emailService, times(1)).sendEmail(eq(RECIPIENT), eq(EmailType.SUCCESS), eq("Austin"));

        // duplicate SQS delivery must not trigger a second email
        service.updateAsyncImageReport(sqs(reqId, "image-file-1"));
        verify(emailService, times(1)).sendEmail(any(), any(), any());
    }

    @Test
    public void failedLegProducesSingleFailureEmail() {
        ReportRequest request = sampleRequest();
        service.generateReportsAsync(request);
        String reqId = request.getReqId();

        SqsResponse excelResponse = new SqsResponse();
        excelResponse.setReqId(reqId);
        excelResponse.setFailed(true);
        service.updateAsyncExcelReport(excelResponse);
        service.updateAsyncPDFReport(sqs(reqId, "pdf-file-1"));
        service.updateAsyncImageReport(sqs(reqId, "image-file-1"));

        assertEquals(ReportStatus.FAILED, store.get(reqId).getExcelReport().getStatus());
        verify(emailService, times(1)).sendEmail(eq(RECIPIENT), eq(EmailType.FAILURE), eq("Austin"));
        verify(emailService, never()).sendEmail(any(), eq(EmailType.SUCCESS), any());
    }

    @Test
    public void updateForUnknownRequestThrows() {
        SqsResponse response = new SqsResponse();
        response.setReqId("does-not-exist");
        assertThrows(RequestNotFoundException.class, () -> service.updateAsyncPDFReport(response));
    }
}
