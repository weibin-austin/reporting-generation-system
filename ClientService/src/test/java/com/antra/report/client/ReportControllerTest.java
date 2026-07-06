package com.antra.report.client;

import com.antra.report.client.controller.ReportController;
import com.antra.report.client.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ReportControllerTest {

    @Mock
    ReportService reportService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportService)).build();
    }

    @Test
    public void listReportReturnsOk() throws Exception {
        when(reportService.getReportList()).thenReturn(List.of());
        mockMvc.perform(get("/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"));
    }

    @Test
    public void requestWithoutSubmitterIsRejected() throws Exception {
        mockMvc.perform(post("/report/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"d\",\"headers\":[\"A\"],\"data\":[[\"1\"]]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void requestWithoutDataIsRejected() throws Exception {
        mockMvc.perform(post("/report/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"d\",\"headers\":[\"A\"],\"submitter\":\"Austin\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void clientSuppliedReqIdIsRejected() throws Exception {
        mockMvc.perform(post("/report/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reqId\":\"Req-hack\",\"description\":\"d\",\"headers\":[\"A\"],\"data\":[[\"1\"]],\"submitter\":\"Austin\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void validAsyncRequestIsAccepted() throws Exception {
        mockMvc.perform(post("/report/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"d\",\"headers\":[\"A\"],\"data\":[[\"1\"]],\"submitter\":\"Austin\"}"))
                .andExpect(status().isOk());
    }

    @Test
    public void updateReportDelegatesToService() throws Exception {
        mockMvc.perform(put("/report/Req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"new desc\"}"))
                .andExpect(status().isOk());
        verify(reportService).updateReport(eq("Req-1"), any());
    }

    @Test
    public void updateWithBlankDescriptionIsRejected() throws Exception {
        mockMvc.perform(put("/report/Req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteReportDelegatesToService() throws Exception {
        mockMvc.perform(delete("/report/Req-1"))
                .andExpect(status().isOk());
        verify(reportService).deleteReport("Req-1");
    }
}
