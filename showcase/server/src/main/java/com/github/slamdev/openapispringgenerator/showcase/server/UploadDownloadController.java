package com.github.slamdev.openapispringgenerator.showcase.server;

import com.github.slamdev.openapispringgenerator.showcase.server.api.DownloadMulti;
import com.github.slamdev.openapispringgenerator.showcase.server.api.UploadDownloadApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class UploadDownloadController implements UploadDownloadApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadDownloadController.class);

    @Override
    public DownloadMulti downloadMultiple() {
        byte[] content = "test-content".getBytes(StandardCharsets.UTF_8);
        return DownloadMulti.builder()
                .username("test")
                .avatar(content)
                .build();
    }

    @Override
    public Resource downloadOne() {
        byte[] content = "test-content".getBytes(StandardCharsets.UTF_8);
        return new ByteArrayResource(content);
    }

    @Override
    public void uploadMultipart(Integer orderId, Integer userId, MultipartFile fileName) {
        LOGGER.info("uploadMultipart: {}", fileName);
    }

    @Override
//    public void uploadMultiple(List<MultipartFile> filenames) {
    public void uploadMultiple(List<Resource> filenames) {
        LOGGER.info("uploadMultiple: {}", filenames);
    }

    @Override
//    public void uploadOne(byte[] body) {
    public void uploadOne(MultipartFile body) {
        LOGGER.info("uploadOne: {}", body);
    }
}
