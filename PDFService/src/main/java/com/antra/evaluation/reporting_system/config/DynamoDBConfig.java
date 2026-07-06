package com.antra.evaluation.reporting_system.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DynamoDBConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBConfig.class);

    // Default (real AWS) client; the "local" profile overrides this with a
    // LocalStack-pointed @Primary bean in LocalStackConfig.
    @Bean
    @Profile("!local")
    public AmazonDynamoDB amazonDynamoDB(@Value("${cloud.aws.region.static:us-east-1}") String region) {
        return AmazonDynamoDBClientBuilder.standard().withRegion(region).build();
    }

    @Bean
    public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB amazonDynamoDB) {
        return new DynamoDBMapper(amazonDynamoDB);
    }

    // Create the PDFFile table on startup if it doesn't exist (idempotent), so
    // local/dev and CI work without a manual provisioning step.
    @Bean
    public ApplicationRunner ensurePdfFileTable(AmazonDynamoDB amazonDynamoDB, DynamoDBMapper mapper,
                                                @Value("${app.dynamodb.auto-create-table:true}") boolean autoCreate) {
        return args -> {
            if (!autoCreate) {
                return;
            }
            CreateTableRequest request = mapper.generateCreateTableRequest(PDFFile.class)
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));
            try {
                boolean created = TableUtils.createTableIfNotExists(amazonDynamoDB, request);
                if (created) {
                    TableUtils.waitUntilActive(amazonDynamoDB, request.getTableName());
                    log.info("Created DynamoDB table {}", request.getTableName());
                } else {
                    log.info("DynamoDB table {} already exists", request.getTableName());
                }
            } catch (ResourceInUseException e) {
                log.info("DynamoDB table {} already exists", request.getTableName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }
}
