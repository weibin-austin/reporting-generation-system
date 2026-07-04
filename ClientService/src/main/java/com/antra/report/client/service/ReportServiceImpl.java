package com.antra.report.client.service;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.report.client.entity.ExcelReportEntity;
import com.antra.report.client.entity.PDFReportEntity;
import com.antra.report.client.entity.ReportRequestEntity;
import com.antra.report.client.entity.ReportStatus;
import com.antra.report.client.exception.RequestNotFoundException;
import com.antra.report.client.pojo.EmailType;
import com.antra.report.client.pojo.FileType;
import com.antra.report.client.pojo.reponse.ExcelResponse;
import com.antra.report.client.pojo.reponse.PDFResponse;
import com.antra.report.client.pojo.reponse.ReportVO;
import com.antra.report.client.pojo.reponse.SqsResponse;
import com.antra.report.client.pojo.request.ReportRequest;
import com.antra.report.client.repository.ReportRequestRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    @Value("${app.notification.to:youremail@gmail.com}")
    private String notificationRecipient;

    @Value("${app.service.excel.url:http://localhost:8888/excel}")
    private String excelServiceUrl;

    @Value("${app.service.pdf.url:http://localhost:9999/pdf}")
    private String pdfServiceUrl;

    private final ReportRequestRepo reportRequestRepo;
    private final SNSService snsService;
    private final AmazonS3 s3Client;
    private final EmailService emailService;
    private final RestTemplate restTemplate;
    // One thread per downstream service so a sync request fans out to both at once
    private final ExecutorService directRequestExecutor = Executors.newFixedThreadPool(2);

    public ReportServiceImpl(ReportRequestRepo reportRequestRepo, SNSService snsService, AmazonS3 s3Client, EmailService emailService, RestTemplate restTemplate) {
        this.reportRequestRepo = reportRequestRepo;
        this.snsService = snsService;
        this.s3Client = s3Client;
        this.emailService = emailService;
        this.restTemplate = restTemplate;
    }

    @PreDestroy
    public void shutdownExecutor() {
        directRequestExecutor.shutdown();
    }

    private ReportRequestEntity persistToLocal(ReportRequest request) {
        request.setReqId("Req-"+ UUID.randomUUID().toString());

        ReportRequestEntity entity = new ReportRequestEntity();
        entity.setReqId(request.getReqId());
        entity.setSubmitter(request.getSubmitter());
        entity.setDescription(request.getDescription());
        entity.setCreatedTime(LocalDateTime.now());

        PDFReportEntity pdfReport = new PDFReportEntity();
        pdfReport.setRequest(entity);
        pdfReport.setStatus(ReportStatus.PENDING);
        pdfReport.setCreatedTime(LocalDateTime.now());
        entity.setPdfReport(pdfReport);

        ExcelReportEntity excelReport = new ExcelReportEntity();
        BeanUtils.copyProperties(pdfReport, excelReport);
        entity.setExcelReport(excelReport);

        return reportRequestRepo.save(entity);
    }

    @Override
    public ReportVO generateReportsSync(ReportRequest request) {
        persistToLocal(request);
        sendDirectRequests(request);
        return new ReportVO(reportRequestRepo.findById(request.getReqId()).orElseThrow());
    }
    // Both downstream services are called concurrently; the sync API latency is
    // max(excel, pdf) instead of their sum.
    private void sendDirectRequests(ReportRequest request) {
        CompletableFuture<Void> excelDone = CompletableFuture.runAsync(() -> {
            ExcelResponse excelResponse = new ExcelResponse();
            try {
                excelResponse = restTemplate.postForEntity(excelServiceUrl, request, ExcelResponse.class).getBody();
            } catch (Exception e) {
                log.error("Excel Generation Error (Sync) : e", e);
                excelResponse.setReqId(request.getReqId());
                excelResponse.setFailed(true);
            } finally {
                updateLocal(excelResponse);
            }
        }, directRequestExecutor);
        CompletableFuture<Void> pdfDone = CompletableFuture.runAsync(() -> {
            PDFResponse pdfResponse = new PDFResponse();
            try {
                pdfResponse = restTemplate.postForEntity(pdfServiceUrl, request, PDFResponse.class).getBody();
            } catch (Exception e) {
                log.error("PDF Generation Error (Sync) : e", e);
                pdfResponse.setReqId(request.getReqId());
                pdfResponse.setFailed(true);
            } finally {
                updateLocal(pdfResponse);
            }
        }, directRequestExecutor);
        CompletableFuture.allOf(excelDone, pdfDone).join();
    }

    private void updateLocal(ExcelResponse excelResponse) {
        SqsResponse response = new SqsResponse();
        BeanUtils.copyProperties(excelResponse, response);
        updateAsyncExcelReport(response);
    }
    private void updateLocal(PDFResponse pdfResponse) {
        SqsResponse response = new SqsResponse();
        BeanUtils.copyProperties(pdfResponse, response);
        updateAsyncPDFReport(response);
    }

    @Override
    @Transactional
    public ReportVO generateReportsAsync(ReportRequest request) {
        ReportRequestEntity entity = persistToLocal(request);
        snsService.sendReportNotification(request);
        log.info("Send SNS the message: {}",request);
        return new ReportVO(entity);
    }

    @Override
//    @Transactional // why this? email could fail
    public void updateAsyncPDFReport(SqsResponse response) {
        ReportRequestEntity entity = reportRequestRepo.findById(response.getReqId()).orElseThrow(RequestNotFoundException::new);
        var pdfReport = entity.getPdfReport();
        pdfReport.setUpdatedTime(LocalDateTime.now());
        if (response.isFailed()) {
            pdfReport.setStatus(ReportStatus.FAILED);
        } else{
            pdfReport.setStatus(ReportStatus.COMPLETED);
            pdfReport.setFileId(response.getFileId());
            pdfReport.setFileLocation(response.getFileLocation());
            pdfReport.setFileSize(response.getFileSize());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        reportRequestRepo.save(entity);
        sendNotificationWhenComplete(entity.getReqId());
    }

    @Override
//    @Transactional
    public void updateAsyncExcelReport(SqsResponse response) {
        ReportRequestEntity entity = reportRequestRepo.findById(response.getReqId()).orElseThrow(RequestNotFoundException::new);
        var excelReport = entity.getExcelReport();
        excelReport.setUpdatedTime(LocalDateTime.now());
        if (response.isFailed()) {
            excelReport.setStatus(ReportStatus.FAILED);
        } else{
            excelReport.setStatus(ReportStatus.COMPLETED);
            excelReport.setFileId(response.getFileId());
            excelReport.setFileLocation(response.getFileLocation());
            excelReport.setFileSize(response.getFileSize());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        reportRequestRepo.save(entity);
        sendNotificationWhenComplete(entity.getReqId());
    }

    // One email per request: fires only once both the PDF and Excel legs have
    // reached a terminal status. Synchronized so the two SQS listener threads
    // can't both pass the notificationSent check.
    private synchronized void sendNotificationWhenComplete(String reqId) {
        ReportRequestEntity entity = reportRequestRepo.findById(reqId).orElseThrow(RequestNotFoundException::new);
        ReportStatus pdfStatus = entity.getPdfReport().getStatus();
        ReportStatus excelStatus = entity.getExcelReport().getStatus();
        if (pdfStatus == ReportStatus.PENDING || excelStatus == ReportStatus.PENDING || entity.isNotificationSent()) {
            return;
        }
        boolean failed = pdfStatus == ReportStatus.FAILED || excelStatus == ReportStatus.FAILED;
        emailService.sendEmail(notificationRecipient, failed ? EmailType.FAILURE : EmailType.SUCCESS, entity.getSubmitter());
        entity.setNotificationSent(true);
        reportRequestRepo.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportVO> getReportList() {
        return reportRequestRepo.findAll().stream().map(ReportVO::new).collect(Collectors.toList());
    }

    @Override
    public InputStream getFileBodyByReqId(String reqId, FileType type) {
        ReportRequestEntity entity = reportRequestRepo.findById(reqId).orElseThrow(RequestNotFoundException::new);
        if (type == FileType.PDF) {
            String fileLocation = entity.getPdfReport().getFileLocation(); // this location is s3 "bucket/key"
            String bucket = fileLocation.split("/")[0];
            String key = fileLocation.split("/")[1];
            return s3Client.getObject(bucket, key).getObjectContent();
        } else if (type == FileType.EXCEL) {
            String fileId = entity.getExcelReport().getFileId();
//            String fileLocation = entity.getExcelReport().getFileLocation();
//            try {
//                return new FileInputStream(fileLocation);// this location is in local, definitely sucks
//            } catch (FileNotFoundException e) {
//                log.error("No file found", e);
//            }
            ResponseEntity<Resource> exchange = restTemplate.exchange(excelServiceUrl + "/{id}/content",
                    HttpMethod.GET, null, Resource.class, fileId);
            try {
                return exchange.getBody().getInputStream();
            } catch (IOException e) {
                log.error("Cannot download excel",e);
            }
        }
        return null;
    }
}
