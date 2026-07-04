package com.antra.evaluation.reporting_system;

import com.antra.evaluation.reporting_system.exception.FileGenerationException;
import com.antra.evaluation.reporting_system.pojo.api.ExcelRequest;
import com.antra.evaluation.reporting_system.pojo.report.ExcelData;
import com.antra.evaluation.reporting_system.pojo.report.ExcelFile;
import com.antra.evaluation.reporting_system.repo.ExcelRepository;
import com.antra.evaluation.reporting_system.service.ExcelGenerationService;
import com.antra.evaluation.reporting_system.service.ExcelServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExcelServiceImplTest {

    @Mock
    ExcelRepository excelRepository;

    @Mock
    ExcelGenerationService excelGenerationService;

    private ExcelServiceImpl service() {
        return new ExcelServiceImpl(excelRepository, excelGenerationService);
    }

    private ExcelRequest sampleRequest() {
        ExcelRequest request = new ExcelRequest();
        request.setDescription("Math Report");
        request.setSubmitter("Austin");
        request.setHeaders(List.of("Name", "Score"));
        request.setData(List.of(List.of("Alice", "100")));
        return request;
    }

    @Test
    public void generateFilePopulatesMetadataAndSaves() throws IOException {
        File temp = File.createTempFile("excel-test", ".xlsx");
        when(excelGenerationService.generateExcelReport(any())).thenReturn(temp);

        ExcelFile info = service().generateFile(sampleRequest(), false);

        assertEquals("Austin", info.getSubmitter());
        assertEquals("Math Report", info.getDescription());
        assertEquals(temp.getAbsolutePath(), info.getFileLocation());
        verify(excelRepository).save(info);

        // regression: the sheet data must carry the request's submitter, not a null copy
        ArgumentCaptor<ExcelData> dataCaptor = ArgumentCaptor.forClass(ExcelData.class);
        verify(excelGenerationService).generateExcelReport(dataCaptor.capture());
        assertEquals("Austin", dataCaptor.getValue().getSubmitter());

        temp.delete();
    }

    @Test
    public void generateFileWrapsGenerationErrors() throws IOException {
        when(excelGenerationService.generateExcelReport(any())).thenThrow(new IOException("disk full"));
        assertThrows(FileGenerationException.class, () -> service().generateFile(sampleRequest(), false));
    }

    @Test
    public void deleteMissingFileThrows() {
        when(excelRepository.findById("nope")).thenReturn(Optional.empty());
        assertThrows(FileNotFoundException.class, () -> service().deleteFile("nope"));
    }

    @Test
    public void deleteRemovesRecordAndPhysicalFile() throws IOException {
        File temp = File.createTempFile("excel-delete-test", ".xlsx");
        ExcelFile stored = new ExcelFile();
        stored.setFileId("file-1");
        stored.setFileLocation(temp.getAbsolutePath());
        when(excelRepository.findById("file-1")).thenReturn(Optional.of(stored));

        ExcelFile deleted = service().deleteFile("file-1");

        assertEquals(stored, deleted);
        verify(excelRepository).deleteById("file-1");
        assertFalse(temp.exists());
    }

    @Test
    public void getExcelBodyByIdMissingThrows() {
        when(excelRepository.findById("nope")).thenReturn(Optional.empty());
        assertThrows(FileNotFoundException.class, () -> service().getExcelBodyById("nope"));
    }
}
