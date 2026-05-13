package com.connecthub.media.service;

import com.connecthub.media.entity.MediaFile;
import com.connecthub.media.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private LocalStorageService localStorageService;

    @InjectMocks
    private MediaServiceImpl mediaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(mediaService, "baseUrl", "http://test-bucket.s3.amazonaws.com");
        ReflectionTestUtils.setField(mediaService, "mockS3", true); // Use mockS3=true to avoid AWS calls
    }

    @Test
    void uploadFile_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Test content".getBytes());
        MediaFile media = new MediaFile();
        media.setOriginalName("test.txt");
        media.setUploaderId("user-1");

        when(mediaRepository.save(any(MediaFile.class))).thenReturn(media);

        MediaFile result = mediaService.uploadFile(file, "user-1", "room-1");

        assertNotNull(result);
        assertEquals("test.txt", result.getOriginalName());
        verify(localStorageService).save(anyString(), any(byte[].class));
        verify(mediaRepository).save(any(MediaFile.class));
    }

    @Test
    void getFileById_Success() {
        MediaFile media = new MediaFile();
        media.setMediaId("media-1");
        when(mediaRepository.findByMediaId("media-1")).thenReturn(Optional.of(media));

        MediaFile result = mediaService.getFileById("media-1");

        assertNotNull(result);
        assertEquals("media-1", result.getMediaId());
    }

    @Test
    void deleteFile_Success() {
        MediaFile media = new MediaFile();
        media.setMediaId("media-1");
        media.setUrl("http://test-bucket.s3.amazonaws.com/files/test.txt");
        
        when(mediaRepository.findByMediaId("media-1")).thenReturn(Optional.of(media));

        mediaService.deleteFile("media-1");

        verify(localStorageService).delete("files/test.txt");
        verify(mediaRepository).deleteByMediaId("media-1");
    }
}
