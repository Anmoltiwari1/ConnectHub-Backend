package com.connecthub.media.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
@Data
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String mediaId;

    private String uploaderId;
    private String roomId;
    private String messageId;
    private String filename;
    private String originalName;
    private String url;
    private String thumbnailUrl;
    private String mimeType;
    private long sizeKb;
    private int width;
    private int height;
    private LocalDateTime uploadedAt;

    @PrePersist
    void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
