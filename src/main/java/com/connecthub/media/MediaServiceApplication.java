package com.connecthub.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@SpringBootApplication
public class MediaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .endpointOverride(URI.create("http://localhost:4566")) // LocalStack default
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .endpointOverride(URI.create("http://localhost:4566"))
                .build();
    }
}
