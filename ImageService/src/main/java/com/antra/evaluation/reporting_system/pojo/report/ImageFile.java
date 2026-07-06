package com.antra.evaluation.reporting_system.pojo.report;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.antra.evaluation.reporting_system.repo.LocalDateTimeConverter;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@DynamoDBTable(tableName = "ImageFile")
public class ImageFile {

    @DynamoDBHashKey
    private String id;

    @DynamoDBAttribute
    private String fileName;

    @DynamoDBAttribute
    private String fileLocation;

    @DynamoDBAttribute
    private String submitter;

    @DynamoDBAttribute
    private Long fileSize;

    @DynamoDBAttribute
    private String description;

    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    @DynamoDBAttribute
    private LocalDateTime generatedTime;
}
