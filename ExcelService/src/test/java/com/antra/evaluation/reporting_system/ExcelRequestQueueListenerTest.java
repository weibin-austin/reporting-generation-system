package com.antra.evaluation.reporting_system;

import com.antra.evaluation.reporting_system.endpoint.ExcelRequestQueueListener;
import com.antra.evaluation.reporting_system.exception.FileGenerationException;
import com.antra.evaluation.reporting_system.pojo.api.ExcelRequest;
import com.antra.evaluation.reporting_system.pojo.api.ExcelResponse;
import com.antra.evaluation.reporting_system.pojo.api.ExcelSNSRequest;
import com.antra.evaluation.reporting_system.pojo.report.ExcelFile;
import com.antra.evaluation.reporting_system.service.ExcelService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExcelRequestQueueListenerTest {

    @Mock
    QueueMessagingTemplate queueMessagingTemplate;

    @Mock
    ExcelService excelService;

    private ExcelRequestQueueListener listener;

    @BeforeEach
    public void setUp() {
        listener = new ExcelRequestQueueListener(queueMessagingTemplate, excelService);
    }

    private ExcelRequest sampleRequest() {
        ExcelRequest request = new ExcelRequest();
        request.setReqId("Req-1");
        return request;
    }

    @Test
    public void successRepliesWithFileInfo() {
        ExcelFile file = new ExcelFile();
        file.setFileId("file-1");
        file.setFileLocation("/tmp/file-1.xlsx");
        file.setFileSize(42L);
        when(excelService.generateFile(any(), anyBoolean())).thenReturn(file);

        listener.queueListener(sampleRequest());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(queueMessagingTemplate).convertAndSend(anyString(), captor.capture());
        ExcelResponse response = (ExcelResponse) captor.getValue();
        assertEquals("Req-1", response.getReqId());
        assertEquals("file-1", response.getFileId());
        assertFalse(response.isFailed());
    }

    @Test
    public void generationFailureRepliesFailedWithoutRetry() {
        when(excelService.generateFile(any(), anyBoolean()))
                .thenThrow(new FileGenerationException("bad data"));

        listener.queueListener(sampleRequest());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(queueMessagingTemplate).convertAndSend(anyString(), captor.capture());
        assertTrue(((ExcelResponse) captor.getValue()).isFailed());
    }

    @Test
    public void unexpectedFailurePropagatesSoSqsRedelivers() {
        when(excelService.generateFile(any(), anyBoolean()))
                .thenThrow(new IllegalStateException("db connection lost"));

        assertThrows(IllegalStateException.class, () -> listener.queueListener(sampleRequest()));

        verify(queueMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    public void deadLetteredRequestRepliesFailedSoReportIsNotStuckPending() {
        ExcelSNSRequest snsRequest = new ExcelSNSRequest();
        snsRequest.setExcelRequest(sampleRequest());

        listener.deadLetterQueueListener(snsRequest);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(queueMessagingTemplate).convertAndSend(anyString(), captor.capture());
        ExcelResponse response = (ExcelResponse) captor.getValue();
        assertEquals("Req-1", response.getReqId());
        assertTrue(response.isFailed());
    }
}
