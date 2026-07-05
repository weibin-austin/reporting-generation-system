package com.antra.evaluation.reporting_system.endpoint;

import com.antra.evaluation.reporting_system.pojo.api.PDFRequest;
import com.antra.evaluation.reporting_system.pojo.api.PDFResponse;
import com.antra.evaluation.reporting_system.pojo.api.PDFSNSRequest;
import com.antra.evaluation.reporting_system.pojo.exception.PDFGenerationException;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import com.antra.evaluation.reporting_system.service.PDFService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Component;

@Component
public class PDFRequestQueueListener {

    private static final Logger log = LoggerFactory.getLogger(PDFRequestQueueListener.class);

    private final QueueMessagingTemplate queueMessagingTemplate;

    private final PDFService pdfService;

    public PDFRequestQueueListener(QueueMessagingTemplate queueMessagingTemplate, PDFService pdfService) {
        this.queueMessagingTemplate = queueMessagingTemplate;
        this.pdfService = pdfService;
    }

    public void queueListener(PDFRequest request) {
        PDFResponse response = new PDFResponse();
        response.setReqId(request.getReqId());

        try {
            PDFFile file = pdfService.createPDF(request);
            response.setFileId(file.getId());
            response.setFileLocation(file.getFileLocation());
            response.setFileSize(file.getFileSize());
            log.info("Generated: {}", file);
        } catch (PDFGenerationException e) {
            // Non-retryable: the request data itself cannot be turned into a file,
            // so redelivering the message would fail the same way. Reply "failed".
            response.setFailed(true);
            log.error("Error in generating pdf", e);
        }
        // Any other exception (S3, Mongo, queue outage...) propagates: the message is
        // redelivered by SQS and moved to the DLQ once maxReceiveCount is exhausted.

        send(response);
        log.info("Replied back: {}", response);
    }

    @SqsListener(value = "PDF_Request_Queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void fanoutQueueListener(PDFSNSRequest request) {
        log.info("Get fanout request: {}", request);
        queueListener(request.getPdfRequest());
    }

    // Retries exhausted: reply "failed" so the report doesn't stay PENDING forever.
    @SqsListener("PDF_Request_Queue_DLQ")
    public void deadLetterQueueListener(PDFSNSRequest request) {
        PDFRequest pdfRequest = request.getPdfRequest();
        log.error("Request exhausted its retries, marking as failed: {}", pdfRequest);
        PDFResponse response = new PDFResponse();
        response.setReqId(pdfRequest.getReqId());
        response.setFailed(true);
        send(response);
    }

    private void send(Object message) {
        queueMessagingTemplate.convertAndSend("PDF_Response_Queue", message);
    }
}
/**
 * {
 *   "description":"Student Math Course Report",
 *   "headers":["Student #","Name","Class","Score"],
 *   "submitter":"Mrs. York1234"
 * }
 **/
