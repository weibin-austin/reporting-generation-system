package com.antra.evaluation.reporting_system.service;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.evaluation.reporting_system.pojo.api.ImageRequest;
import com.antra.evaluation.reporting_system.pojo.exception.ImageGenerationException;
import com.antra.evaluation.reporting_system.pojo.report.ImageFile;
import com.antra.evaluation.reporting_system.repo.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ImageServiceImpl implements ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageServiceImpl.class);

    private final ImageRepository repository;
    private final ImageGenerator generator;
    private final AmazonS3 s3Client;

    @Value("${s3.bucket}")
    private String s3Bucket;

    public ImageServiceImpl(ImageRepository repository, ImageGenerator generator, AmazonS3 s3Client) {
        this.repository = repository;
        this.generator = generator;
        this.s3Client = s3Client;
    }

    @Override
    public ImageFile createImage(ImageRequest request) {
        ImageFile file = new ImageFile();
        file.setId("Image-" + UUID.randomUUID());
        file.setSubmitter(request.getSubmitter());
        file.setDescription(request.getDescription());
        file.setGeneratedTime(LocalDateTime.now());

        File temp;
        try {
            temp = generator.generate(request);
        } catch (IOException e) {
            throw new ImageGenerationException(e);
        }

        log.debug("Upload temp image to s3 {}", temp.getAbsolutePath());
        s3Client.putObject(s3Bucket, file.getId(), temp);

        file.setFileLocation(String.join("/", s3Bucket, file.getId()));
        file.setFileSize(temp.length());
        file.setFileName(file.getId() + ".png");
        repository.save(file);

        if (temp.delete()) {
            log.debug("cleared temp image {}", temp.getName());
        }
        return file;
    }

    @Override
    public void deleteImage(String id) {
        // Idempotent: nothing to do if the metadata is already gone.
        repository.findById(id).ifPresent(file -> {
            String[] parts = file.getFileLocation().split("/", 2); // "bucket/key"
            if (parts.length == 2) {
                s3Client.deleteObject(parts[0], parts[1]);
            }
            repository.delete(file);
            log.debug("Deleted image {} and its S3 object", id);
        });
    }
}
