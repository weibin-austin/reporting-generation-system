package com.antra.evaluation.reporting_system.repo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

import java.time.LocalDateTime;

/**
 * DynamoDB has no native date type, so LocalDateTime is stored as an ISO-8601 string.
 */
public class LocalDateTimeConverter implements DynamoDBTypeConverter<String, LocalDateTime> {

    @Override
    public String convert(LocalDateTime source) {
        return source == null ? null : source.toString();
    }

    @Override
    public LocalDateTime unconvert(String source) {
        return source == null ? null : LocalDateTime.parse(source);
    }
}
