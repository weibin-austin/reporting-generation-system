package com.antra.report.client.service;

import com.antra.report.client.pojo.FileType;
import com.antra.report.client.pojo.reponse.ReportVO;
import com.antra.report.client.pojo.reponse.SqsResponse;
import com.antra.report.client.pojo.request.ReportRequest;
import com.antra.report.client.pojo.request.UpdateReportRequest;

import java.io.InputStream;
import java.util.List;

public interface ReportService {
    ReportVO generateReportsSync(ReportRequest request);

    ReportVO generateReportsAsync(ReportRequest request);

    void updateAsyncPDFReport(SqsResponse response);

    void updateAsyncExcelReport(SqsResponse response);

    void updateAsyncImageReport(SqsResponse response);

    List<ReportVO> getReportList();

    InputStream getFileBodyByReqId(String reqId, FileType type);

    ReportVO updateReport(String reqId, UpdateReportRequest request);

    void deleteReport(String reqId);
}
