package com.connecthub.media.repository;

import com.connecthub.media.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<MediaFile, String> {

    List<MediaFile> findByUploaderId(String uploaderId);
    List<MediaFile> findByRoomId(String roomId);
    Optional<MediaFile> findByMessageId(String messageId);
    Optional<MediaFile> findByMediaId(String mediaId);
    List<MediaFile> findByMimeTypeContaining(String mimeType);
    long countByRoomId(String roomId);
    void deleteByMediaId(String mediaId);
}
