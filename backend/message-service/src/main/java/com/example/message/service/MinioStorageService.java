package com.example.message.service;

import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Uploads escalation PDF files to MinIO and returns a public URL.
 */
@Service
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket:escalation-reports}")
    private String bucket;

    @Value("${minio.base-url:http://localhost:9000}")
    private String minioBaseUrl;

    public MinioStorageService(
            @Value("${minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${minio.access-key:}") String accessKey,
            @Value("${minio.secret-key:}") String secretKey) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Uploads the local PDF file at {@code localPath} to MinIO and returns
     * the public URL.
     *
     * @param localPath local path to the PDF file
     * @return public URL pointing to the uploaded object
     */
    public String upload(Path localPath) throws Exception {
        String objectName = localPath.getFileName().toString();
        log.info("[MinIO] Uploading escalation report: {}", objectName);
        minioClient.uploadObject(UploadObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .filename(localPath.toString())
                .contentType("application/pdf")
                .build());
        String url = minioBaseUrl + "/" + bucket + "/" + objectName;
        log.info("[MinIO] Upload complete: {}", url);
        return url;
    }
}
