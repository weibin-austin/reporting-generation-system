package com.antra.evaluation.reporting_system;

import com.antra.evaluation.reporting_system.endpoint.ImageRequestQueueListener;
import com.antra.evaluation.reporting_system.pojo.api.ImageRequest;
import com.antra.evaluation.reporting_system.pojo.api.ImageResponse;
import com.antra.evaluation.reporting_system.pojo.api.ImageSNSRequest;
import com.antra.evaluation.reporting_system.pojo.exception.ImageGenerationException;
import com.antra.evaluation.reporting_system.pojo.report.ImageFile;
import com.antra.evaluation.reporting_system.service.ImageService;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ImageRequestQueueListenerTest {

    @Mock
    QueueMessagingTemplate queueMessagingTemplate;
    @Mock
    ImageService imageService;

    private ImageRequestQueueListener listener;

    @BeforeEach
    public void setUp() {
        listener = new ImageRequestQueueListener(queueMessagingTemplate, imageService);
    }

    private ImageRequest sampleRequest() {
        ImageRequest request = new ImageRequest();
        request.setReqId("Req-1");
        return request;
    }

    @Test
    public void successRepliesWithFileInfo() {
        ImageFile file = new ImageFile();
        file.setId("Image-1");
        file.setFileLocation("bucket/Image-1");
        file.setFileSize(42L);
        when(imageService.createImage(any())).thenReturn(file);

        listener.queueListener(sampleRequest());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(queueMessagingTemplate).convertAndSend(anyString(), captor.capture());
        ImageResponse response = (ImageResponse) captor.getValue();
        assertEquals("Req-1", response.getReqId());
        assertEquals("Image-1", response.getFileId());
        assertFalse(response.isFailed());
    }

    @Test
    public void generationFailureRepliesFailedWithoutRetry() {
        when(imageService.createImage(any())).thenThrow(new ImageGenerationException(new RuntimeException("bad data")));

        listener.queueListener(sampleRequest());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(queueMessagingTemplate).convertAndSend(anyString(), captor.capture());
        assertTrue(((ImageResponse) captor.getValue()).isFailed());
    }

    @Test
    public void unexpectedFailurePropagatesSoSqsRedelivers() {
        when(imageService.createImage(any())).thenThrow(new IllegalStateException("s3 down"));

        assertThrows(IllegalStateException.class, () -> listener.queueListener(sampleRequest()));

        verify(queueMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    public void deadLetteredRequestRepliesFailedSoReportIsNotStuckPending() {
        ImageSNSRequest snsRequest = new ImageSNSRequest();
        snsRequest.setImageRequest(sampleRequest());

        listener.deadLetterQueueListener(snsRequest);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(queueMessagingTemplate).convertAndSend(anyString(), captor.capture());
        ImageResponse response = (ImageResponse) captor.getValue();
        assertEquals("Req-1", response.getReqId());
        assertTrue(response.isFailed());
    }
}
