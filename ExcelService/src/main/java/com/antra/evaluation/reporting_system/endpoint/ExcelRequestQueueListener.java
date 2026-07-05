package com.antra.evaluation.reporting_system.endpoint;

import com.antra.evaluation.reporting_system.exception.FileGenerationException;
import com.antra.evaluation.reporting_system.pojo.api.ExcelRequest;
import com.antra.evaluation.reporting_system.pojo.api.ExcelResponse;
import com.antra.evaluation.reporting_system.pojo.api.ExcelSNSRequest;
import com.antra.evaluation.reporting_system.pojo.report.ExcelFile;
import com.antra.evaluation.reporting_system.service.ExcelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Component;

@Component
public class ExcelRequestQueueListener {

    private static final Logger log = LoggerFactory.getLogger(ExcelRequestQueueListener.class);

    private final QueueMessagingTemplate queueMessagingTemplate;

    private final ExcelService excelService;

    public ExcelRequestQueueListener(QueueMessagingTemplate queueMessagingTemplate, ExcelService excelService) {
        this.queueMessagingTemplate = queueMessagingTemplate;
        this.excelService = excelService;
    }

    public void queueListener(ExcelRequest request) {
        ExcelResponse response = new ExcelResponse();
        response.setReqId(request.getReqId());

        try {
            ExcelFile file = excelService.generateFile(request, false);
            response.setFileId(file.getFileId());
            response.setFileLocation(file.getFileLocation());
            response.setFileSize(file.getFileSize());
            log.info("Generated: {}", file);
        } catch (FileGenerationException e) {
            // Non-retryable: the request data itself cannot be turned into a file,
            // so redelivering the message would fail the same way. Reply "failed".
            response.setFailed(true);
            log.error("Error in generating excel", e);
        }
        // Any other exception (disk, DB, queue outage...) propagates: the message is
        // redelivered by SQS and moved to the DLQ once maxReceiveCount is exhausted.

        send(response);
        log.info("Replied back: {}", response);
    }

    @SqsListener(value = "Excel_Request_Queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void fanoutQueueListener(ExcelSNSRequest request) {
        log.info("Get fanout request: {}", request);
        queueListener(request.getExcelRequest());
    }

    // Retries exhausted: reply "failed" so the report doesn't stay PENDING forever.
    @SqsListener("Excel_Request_Queue_DLQ")
    public void deadLetterQueueListener(ExcelSNSRequest request) {
        ExcelRequest excelRequest = request.getExcelRequest();
        log.error("Request exhausted its retries, marking as failed: {}", excelRequest);
        ExcelResponse response = new ExcelResponse();
        response.setReqId(excelRequest.getReqId());
        response.setFailed(true);
        send(response);
    }

    private void send(Object message) {
        queueMessagingTemplate.convertAndSend("Excel_Response_Queue", message);
    }
}
/**
 * {
 *   "description":"Student Math Course Report",
 *   "headers":["Student #","Name","Class","Score"],
 *   "submitter":"Mrs. York1234"
 * }
 **/
