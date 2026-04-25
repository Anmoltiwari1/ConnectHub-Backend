package com.connecthub.media.service;

import com.connecthub.media.entity.MediaFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {

    MediaFile uploadFile(MultipartFile file, String uploaderId, String roomId);
    MediaFile uploadImage(MultipartFile image, String uploaderId, String roomId);
    MediaFile getFileById(String mediaId);
    List<MediaFile> getFilesByRoom(String roomId);
    List<MediaFile> getFilesByUploader(String uploaderId);
    void deleteFile(String mediaId);
    String generateThumbnail(MultipartFile image);
    List<MediaFile> getAllFiles();
    long getFileCount(String roomId);
}
