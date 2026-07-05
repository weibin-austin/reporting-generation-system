package com.antra.evaluation.reporting_system;

import com.antra.evaluation.reporting_system.endpoint.PDFRequestQueueListener;
import com.antra.evaluation.reporting_system.pojo.api.PDFRequest;
import com.antra.evaluation.reporting_system.pojo.api.PDFResponse;
import com.antra.evaluation.reporting_system.pojo.api.PDFSNSRequest;
import com.antra.evaluation.reporting_system.pojo.exception.PDFGenerationException;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import com.antra.evaluation.reporting_system.service.PDFService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;

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
public class PDFRequestQueueListenerTest {

    @Mock
    QueueMessagingTemplate queueMessagingTemplate;

    @Mock
    PDFService pdfService;

    private PDFRequestQueueListener listener;

    @BeforeEach
    public void setUp() {
        listener = new PDFRequestQueueListener(queueMessagingTemplate, pdfService);
    }

    private PDFRequest sampleRequest() {
        PDFRequest request = new PDFRequest();
        request.setReqId("Req-1");
        return request;
    }

    @Test
    public void successRepliesWithFileInfo() {
        PDFFile file = new PDFFile();
        file.setId("file-1");
        file.setFileLocation("bucket/file-1");
        file.setFileSize(42L);
        when(pdfService.createPDF(any())).thenReturn(file);

        listener.queueListener(sampleRequest());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(queueMessagingTemplate).convertAndSend(anyString(), captor.capture());
        PDFResponse response = (PDFResponse) captor.getValue();
        assertEquals("Req-1", response.getReqId());
        assertEquals("file-1", response.getFileId());
        assertFalse(response.isFailed());
    }

    @Test
    public void generationFailureRepliesFailedWithoutRetry() {
        when(pdfService.createPDF(any())).thenThrow(new PDFGenerationException());

        listener.queueListener(sampleRequest());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(queueMessagingTemplate).convertAndSend(anyString(), captor.capture());
        assertTrue(((PDFResponse) captor.getValue()).isFailed());
    }

    @Test
    public void unexpectedFailurePropagatesSoSqsRedelivers() {
        when(pdfService.createPDF(any())).thenThrow(new IllegalStateException("mongo down"));

        assertThrows(IllegalStateException.class, () -> listener.queueListener(sampleRequest()));

        verify(queueMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    public void deadLetteredRequestRepliesFailedSoReportIsNotStuckPending() {
        PDFSNSRequest snsRequest = new PDFSNSRequest();
        snsRequest.setPdfRequest(sampleRequest());

        listener.deadLetterQueueListener(snsRequest);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(queueMessagingTemplate).convertAndSend(anyString(), captor.capture());
        PDFResponse response = (PDFResponse) captor.getValue();
        assertEquals("Req-1", response.getReqId());
        assertTrue(response.isFailed());
    }
}
