package com.connecthub.media.service;

import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class LocalStorageService {

    private static final String UPLOAD_DIR = "uploads/";

    public LocalStorageService() {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) dir.mkdirs();
        new File(UPLOAD_DIR + "files").mkdirs();
        new File(UPLOAD_DIR + "images").mkdirs();
        new File(UPLOAD_DIR + "thumbnails").mkdirs();
    }

    public void save(String key, byte[] content) throws IOException {
        Path path = Paths.get(UPLOAD_DIR + key);
        Files.createDirectories(path.getParent());
        Files.write(path, content);
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(Paths.get(UPLOAD_DIR + key));
        } catch (IOException e) {
            // Log or handle error
        }
    }
}
