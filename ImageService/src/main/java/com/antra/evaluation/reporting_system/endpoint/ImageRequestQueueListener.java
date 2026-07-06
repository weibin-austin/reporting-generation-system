package com.antra.evaluation.reporting_system.endpoint;

import com.antra.evaluation.reporting_system.pojo.api.ImageRequest;
import com.antra.evaluation.reporting_system.pojo.api.ImageResponse;
import com.antra.evaluation.reporting_system.pojo.api.ImageSNSRequest;
import com.antra.evaluation.reporting_system.pojo.exception.ImageGenerationException;
import com.antra.evaluation.reporting_system.pojo.report.ImageFile;
import com.antra.evaluation.reporting_system.service.ImageService;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ImageRequestQueueListener {

    private static final Logger log = LoggerFactory.getLogger(ImageRequestQueueListener.class);

    private final QueueMessagingTemplate queueMessagingTemplate;
    private final ImageService imageService;

    public ImageRequestQueueListener(QueueMessagingTemplate queueMessagingTemplate, ImageService imageService) {
        this.queueMessagingTemplate = queueMessagingTemplate;
        this.imageService = imageService;
    }

    public void queueListener(ImageRequest request) {
        ImageResponse response = new ImageResponse();
        response.setReqId(request.getReqId());
        try {
            ImageFile file = imageService.createImage(request);
            response.setFileId(file.getId());
            response.setFileLocation(file.getFileLocation());
            response.setFileSize(file.getFileSize());
            log.info("Generated: {}", file);
        } catch (ImageGenerationException e) {
            // Non-retryable: the request data itself can't be turned into an image.
            response.setFailed(true);
            log.error("Error in generating image", e);
        }
        // Any other exception (S3, DynamoDB, queue outage...) propagates: SQS
        // redelivers and moves the message to the DLQ once maxReceiveCount is hit.
        send(response);
        log.info("Replied back: {}", response);
    }

    @SqsListener(value = "Image_Request_Queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void fanoutQueueListener(ImageSNSRequest request) {
        log.info("Get fanout request: {}", request);
        queueListener(request.getImageRequest());
    }

    // Retries exhausted: reply "failed" so the report doesn't stay PENDING forever.
    @SqsListener("Image_Request_Queue_DLQ")
    public void deadLetterQueueListener(ImageSNSRequest request) {
        ImageRequest imageRequest = request.getImageRequest();
        log.error("Request exhausted its retries, marking as failed: {}", imageRequest);
        ImageResponse response = new ImageResponse();
        response.setReqId(imageRequest.getReqId());
        response.setFailed(true);
        send(response);
    }

    private void send(Object message) {
        queueMessagingTemplate.convertAndSend("Image_Response_Queue", message);
    }
}
