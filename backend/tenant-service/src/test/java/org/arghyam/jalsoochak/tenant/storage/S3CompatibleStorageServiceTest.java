package org.arghyam.jalsoochak.tenant.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.arghyam.jalsoochak.tenant.config.properties.StorageProperties;
import org.arghyam.jalsoochak.tenant.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3CompatibleStorageService Tests")
class S3CompatibleStorageServiceTest {

    @Mock
    private S3Client s3Client;

    private S3CompatibleStorageService service;

    private static final String BUCKET = "tenant-assets";
    private static final String OBJECT_KEY = "logos/1/abc123.png";

    @BeforeEach
    void setUp() {
        StorageProperties props = new StorageProperties();
        props.setEndpoint("http://localhost:9000");
        props.setRegion("us-east-1");
        props.setAccessKey("test-access-key");
        props.setSecretKey("test-secret-key");
        props.setBucket(BUCKET);
        service = new S3CompatibleStorageService(s3Client, props);
    }

    @Nested
    @DisplayName("upload")
    class UploadTests {

        @Test
        void upload_success_returnsObjectKey() {
            byte[] content = "fake-image".getBytes();
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(null);

            String result = service.upload(OBJECT_KEY,
                    new ByteArrayInputStream(content), content.length, "image/png");

            assertThat(result).isEqualTo(OBJECT_KEY);
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        void upload_s3Failure_throwsStorageException() {
            byte[] content = "fake-image".getBytes();
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(S3Exception.builder().message("Access denied").build());

            assertThatThrownBy(() -> service.upload(OBJECT_KEY,
                    new ByteArrayInputStream(content), content.length, "image/png"))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining(OBJECT_KEY);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        void delete_success() {
            service.delete(OBJECT_KEY);
            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        void delete_noSuchKey_silentlyIgnored() {
            doThrow(NoSuchKeyException.builder().message("Not found").build())
                    .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

            // should not throw
            service.delete(OBJECT_KEY);
            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        void delete_s3Failure_throwsStorageException() {
            doThrow(S3Exception.builder().message("Internal error").build())
                    .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

            assertThatThrownBy(() -> service.delete(OBJECT_KEY))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining(OBJECT_KEY);
        }
    }

    @Nested
    @DisplayName("download")
    class DownloadTests {

        @Test
        void download_success_returnsInputStream() {
            ResponseInputStream<GetObjectResponse> fakeStream = new ResponseInputStream<>(
                    GetObjectResponse.builder().build(),
                    new ByteArrayInputStream("fake-image".getBytes()));
            when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(fakeStream);

            InputStream result = service.download(OBJECT_KEY);

            assertThat(result).isNotNull();
            verify(s3Client).getObject(any(GetObjectRequest.class));
        }

        @Test
        void download_objectNotFound_throwsStorageException() {
            doThrow(NoSuchKeyException.builder().message("Not found").build())
                    .when(s3Client).getObject(any(GetObjectRequest.class));

            assertThatThrownBy(() -> service.download(OBJECT_KEY))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining(OBJECT_KEY);
        }

        @Test
        void download_s3Failure_throwsStorageException() {
            doThrow(S3Exception.builder().message("Internal error").build())
                    .when(s3Client).getObject(any(GetObjectRequest.class));

            assertThatThrownBy(() -> service.download(OBJECT_KEY))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining(OBJECT_KEY);
        }
    }
}
