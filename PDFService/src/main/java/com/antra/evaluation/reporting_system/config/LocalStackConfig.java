package com.antra.evaluation.reporting_system.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * AWS clients pointing to a LocalStack endpoint, so the whole system can run
 * locally without a real AWS account. Active only with the "local" profile.
 */
@Configuration
@Profile("local")
public class LocalStackConfig {

    @Value("${app.aws.endpoint:http://localhost:4566}")
    private String endpoint;

    @Value("${cloud.aws.region.static:us-east-1}")
    private String region;

    private AwsClientBuilder.EndpointConfiguration endpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration(endpoint, region);
    }

    private AWSStaticCredentialsProvider credentials() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials("test", "test"));
    }

    @Bean
    @Primary
    public AmazonSQSAsync amazonSQSAsync() {
        return AmazonSQSAsyncClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration())
                .withCredentials(credentials())
                .build();
    }

    @Bean
    @Primary
    public AmazonS3 amazonS3() {
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration())
                .withCredentials(credentials())
                .withPathStyleAccessEnabled(true)
                .build();
    }
}
