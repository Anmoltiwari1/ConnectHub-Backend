package com.connecthub.media.resource;

import com.connecthub.media.entity.MediaFile;
import com.connecthub.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaResource {

    private final MediaService mediaService;

    @PostMapping("/upload/file")
    public ResponseEntity<MediaFile> uploadFile(@RequestParam MultipartFile file,
                                                 @RequestParam String uploaderId,
                                                 @RequestParam String roomId) {
        return ResponseEntity.ok(mediaService.uploadFile(file, uploaderId, roomId));
    }

    @PostMapping("/upload/image")
    public ResponseEntity<MediaFile> uploadImage(@RequestParam MultipartFile image,
                                                  @RequestParam String uploaderId,
                                                  @RequestParam String roomId) {
        return ResponseEntity.ok(mediaService.uploadImage(image, uploaderId, roomId));
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaFile> getFile(@PathVariable String mediaId) {
        return ResponseEntity.ok(mediaService.getFileById(mediaId));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<MediaFile>> getFilesByRoom(@PathVariable String roomId) {
        return ResponseEntity.ok(mediaService.getFilesByRoom(roomId));
    }

    @GetMapping("/uploader/{uploaderId}")
    public ResponseEntity<List<MediaFile>> getFilesByUploader(@PathVariable String uploaderId) {
        return ResponseEntity.ok(mediaService.getFilesByUploader(uploaderId));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteFile(@PathVariable String mediaId) {
        mediaService.deleteFile(mediaId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/room/{roomId}/count")
    public ResponseEntity<Long> getCount(@PathVariable String roomId) {
        return ResponseEntity.ok(mediaService.getFileCount(roomId));
    }

    @GetMapping
    public ResponseEntity<List<MediaFile>> getAllFiles() {
        return ResponseEntity.ok(mediaService.getAllFiles());
    }
}
