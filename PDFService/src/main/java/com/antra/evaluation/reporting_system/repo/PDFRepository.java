package com.antra.evaluation.reporting_system.repo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Stores PDF file metadata in DynamoDB (was MongoDB). Thin wrapper over
 * DynamoDBMapper so the service layer stays persistence-agnostic.
 */
@Repository
public class PDFRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public PDFRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public PDFFile save(PDFFile file) {
        dynamoDBMapper.save(file);
        return file;
    }

    public Optional<PDFFile> findById(String id) {
        return Optional.ofNullable(dynamoDBMapper.load(PDFFile.class, id));
    }

    public void delete(PDFFile file) {
        dynamoDBMapper.delete(file);
    }
}
