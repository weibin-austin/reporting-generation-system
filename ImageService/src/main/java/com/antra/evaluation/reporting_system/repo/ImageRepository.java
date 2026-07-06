package com.antra.evaluation.reporting_system.repo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.antra.evaluation.reporting_system.pojo.report.ImageFile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Stores image file metadata in DynamoDB. Thin wrapper over DynamoDBMapper so
 * the service layer stays persistence-agnostic.
 */
@Repository
public class ImageRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public ImageRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public ImageFile save(ImageFile file) {
        dynamoDBMapper.save(file);
        return file;
    }

    public Optional<ImageFile> findById(String id) {
        return Optional.ofNullable(dynamoDBMapper.load(ImageFile.class, id));
    }

    public void delete(ImageFile file) {
        dynamoDBMapper.delete(file);
    }
}
