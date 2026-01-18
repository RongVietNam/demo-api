package com.example.demo.common;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FileUtils {

    public static String readContent(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}
