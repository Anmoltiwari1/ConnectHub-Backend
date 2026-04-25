package com.connecthub.media.service;

import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class LocalStorageService {

    private final String uploadDir = "uploads/";

    public LocalStorageService() {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        new File(uploadDir + "files").mkdirs();
        new File(uploadDir + "images").mkdirs();
        new File(uploadDir + "thumbnails").mkdirs();
    }

    public void save(String key, byte[] content) throws IOException {
        Path path = Paths.get(uploadDir + key);
        Files.createDirectories(path.getParent());
        Files.write(path, content);
    }

    public void delete(String key) {
        File file = new File(uploadDir + key);
        if (file.exists()) file.delete();
    }
}
