package com.antra.evaluation.reporting_system;

import com.antra.evaluation.reporting_system.pojo.report.ExcelFile;
import com.antra.evaluation.reporting_system.repo.ExcelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class ExcelRepositoryTest {

    @Autowired
    ExcelRepository excelRepository;

    private ExcelFile sampleFile(String id) {
        ExcelFile file = new ExcelFile();
        file.setFileId(id);
        file.setFileName(id + ".xlsx");
        file.setFileLocation("/tmp/" + id + ".xlsx");
        file.setSubmitter("Austin");
        file.setDescription("Test file");
        file.setFileSize(123L);
        file.setGeneratedTime(LocalDateTime.now());
        return file;
    }

    @Test
    public void saveAndFindRoundTrip() {
        excelRepository.save(sampleFile("file-1"));

        Optional<ExcelFile> found = excelRepository.findById("file-1");
        assertTrue(found.isPresent());
        assertEquals("file-1.xlsx", found.get().getFileName());
        assertEquals("Austin", found.get().getSubmitter());
    }

    @Test
    public void findAllReturnsEverySavedFile() {
        excelRepository.save(sampleFile("file-1"));
        excelRepository.save(sampleFile("file-2"));

        assertEquals(2, excelRepository.findAll().size());
    }

    @Test
    public void deleteRemovesRecord() {
        excelRepository.save(sampleFile("file-1"));
        excelRepository.deleteById("file-1");

        assertTrue(excelRepository.findById("file-1").isEmpty());
    }
}
