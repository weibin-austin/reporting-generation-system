package com.antra.evaluation.reporting_system;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import com.antra.evaluation.reporting_system.repo.PDFRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PDFRepositoryTest {

    @Mock
    DynamoDBMapper dynamoDBMapper;

    @Test
    public void savePersistsViaMapperAndReturnsFile() {
        PDFRepository repository = new PDFRepository(dynamoDBMapper);
        PDFFile file = new PDFFile();
        file.setId("File-1");

        PDFFile saved = repository.save(file);

        assertEquals(file, saved);
        verify(dynamoDBMapper).save(file);
    }

    @Test
    public void findByIdReturnsPresentWhenFound() {
        PDFRepository repository = new PDFRepository(dynamoDBMapper);
        PDFFile file = new PDFFile();
        file.setId("File-1");
        when(dynamoDBMapper.load(PDFFile.class, "File-1")).thenReturn(file);

        Optional<PDFFile> found = repository.findById("File-1");

        assertTrue(found.isPresent());
        assertEquals("File-1", found.get().getId());
    }

    @Test
    public void findByIdReturnsEmptyWhenMissing() {
        PDFRepository repository = new PDFRepository(dynamoDBMapper);
        when(dynamoDBMapper.load(PDFFile.class, "nope")).thenReturn(null);

        assertFalse(repository.findById("nope").isPresent());
    }
}
