package com.antra.evaluation.reporting_system;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.evaluation.reporting_system.pojo.api.ImageRequest;
import com.antra.evaluation.reporting_system.pojo.report.ImageFile;
import com.antra.evaluation.reporting_system.repo.ImageRepository;
import com.antra.evaluation.reporting_system.service.ImageGenerator;
import com.antra.evaluation.reporting_system.service.ImageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ImageServiceImplTest {

    @Mock
    ImageRepository repository;
    @Mock
    ImageGenerator generator;
    @Mock
    AmazonS3 s3Client;

    @Test
    public void createImageUploadsToS3AndCleansUpTempFile() throws IOException {
        ImageServiceImpl service = new ImageServiceImpl(repository, generator, s3Client);
        ReflectionTestUtils.setField(service, "s3Bucket", "test-bucket");

        File temp = File.createTempFile("image-service-test", ".png");
        when(generator.generate(any())).thenReturn(temp);

        ImageRequest request = new ImageRequest();
        request.setDescription("Math Report");
        request.setSubmitter("Austin");
        request.setHeaders(List.of("Name"));
        request.setData(List.of(List.of("Alice")));

        ImageFile result = service.createImage(request);

        assertNotNull(result.getId());
        assertEquals("Austin", result.getSubmitter());
        assertEquals("test-bucket/" + result.getId(), result.getFileLocation());
        verify(s3Client).putObject(eq("test-bucket"), eq(result.getId()), any(File.class));
        verify(repository).save(result);
        assertFalse(temp.exists());
    }

    @Test
    public void deleteImageRemovesS3ObjectAndMetadata() {
        ImageServiceImpl service = new ImageServiceImpl(repository, generator, s3Client);
        ImageFile file = new ImageFile();
        file.setId("Image-1");
        file.setFileLocation("reporting-generated-file/Image-1");
        when(repository.findById("Image-1")).thenReturn(Optional.of(file));

        service.deleteImage("Image-1");

        verify(s3Client).deleteObject("reporting-generated-file", "Image-1");
        verify(repository).delete(file);
    }

    @Test
    public void deleteImageIsNoOpWhenMetadataMissing() {
        ImageServiceImpl service = new ImageServiceImpl(repository, generator, s3Client);
        when(repository.findById("nope")).thenReturn(Optional.empty());

        service.deleteImage("nope");

        verifyNoInteractions(s3Client);
        verify(repository, never()).delete(any());
    }
}
