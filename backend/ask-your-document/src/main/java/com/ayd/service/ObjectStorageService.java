package com.ayd.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface ObjectStorageService {
    void upload(String objectId, MultipartFile file);
    InputStream download(String objectId);
    void delete(String objectId);
}
