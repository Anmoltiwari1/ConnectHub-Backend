package com.connecthub.media.service;

import com.connecthub.media.entity.MediaFile;
import com.connecthub.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final LocalStorageService localStorageService;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.base-url}")
    private String baseUrl;

    @Value("${aws.s3.mock:false}")
    private boolean mockS3;

    @Override
    public MediaFile uploadFile(MultipartFile file, String uploaderId, String roomId) {
        String key = "files/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        saveFile(key, file);
        MediaFile media = buildMediaFile(file, uploaderId, roomId, key, null);
        return mediaRepository.save(media);
    }

    @Override
    public MediaFile uploadImage(MultipartFile image, String uploaderId, String roomId) {
        String key = "images/" + UUID.randomUUID() + "-" + image.getOriginalFilename();
        saveFile(key, image);
        String thumbnailUrl = generateThumbnail(image);
        MediaFile media = buildMediaFile(image, uploaderId, roomId, key, thumbnailUrl);
        return mediaRepository.save(media);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaFile getFileById(String mediaId) {
        return mediaRepository.findByMediaId(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaFile> getFilesByRoom(String roomId) {
        return mediaRepository.findByRoomId(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaFile> getFilesByUploader(String uploaderId) {
        return mediaRepository.findByUploaderId(uploaderId);
    }

    @Override
    public void deleteFile(String mediaId) {
        mediaRepository.findByMediaId(mediaId).ifPresent(media -> {
            String key = extractKey(media.getUrl());
            if (mockS3) {
                localStorageService.delete(key);
            } else {
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            }
            mediaRepository.deleteByMediaId(mediaId);
        });
    }

    @Override
    public String generateThumbnail(MultipartFile image) {
        try {
            String thumbKey = "thumbnails/" + UUID.randomUUID() + "-thumb.jpg";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(image.getInputStream()).size(200, 200).outputFormat("jpg").toOutputStream(out);
            byte[] thumbData = out.toByteArray();

            if (mockS3) {
                localStorageService.save(thumbKey, thumbData);
            } else {
                s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(thumbKey)
                        .contentType("image/jpeg").build(), RequestBody.fromBytes(thumbData));
            }
            return baseUrl + "/" + thumbKey;
        } catch (IOException e) {
            throw new RuntimeException("Thumbnail generation failed", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaFile> getAllFiles() {
        return mediaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public long getFileCount(String roomId) {
        return mediaRepository.countByRoomId(roomId);
    }

    private void saveFile(String key, MultipartFile file) {
        try {
            if (mockS3) {
                localStorageService.save(key, file.getBytes());
            } else {
                s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                        .contentType(file.getContentType()).build(),
                        RequestBody.fromBytes(file.getBytes()));
            }
        } catch (IOException e) {
            throw new RuntimeException("File save failed", e);
        }
    }

    private MediaFile buildMediaFile(MultipartFile file, String uploaderId, String roomId,
                                     String key, String thumbnailUrl) {
        MediaFile media = new MediaFile();
        media.setUploaderId(uploaderId);
        media.setRoomId(roomId);
        media.setOriginalName(file.getOriginalFilename());
        media.setFilename(key);
        media.setUrl(baseUrl + "/" + key);
        media.setThumbnailUrl(thumbnailUrl);
        media.setMimeType(file.getContentType());
        media.setSizeKb(file.getSize() / 1024);
        return media;
    }

    private String extractKey(String url) {
        return url.replace(baseUrl + "/", "");
    }
}
