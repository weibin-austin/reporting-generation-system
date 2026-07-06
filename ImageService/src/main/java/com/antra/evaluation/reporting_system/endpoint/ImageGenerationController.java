package com.antra.evaluation.reporting_system.endpoint;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.antra.evaluation.reporting_system.pojo.api.ImageRequest;
import com.antra.evaluation.reporting_system.pojo.api.ImageResponse;
import com.antra.evaluation.reporting_system.pojo.report.ImageFile;
import com.antra.evaluation.reporting_system.repo.ImageRepository;
import com.antra.evaluation.reporting_system.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;

@RestController
public class ImageGenerationController {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationController.class);

    private final ImageService imageService;
    private final ImageRepository imageRepository;
    private final AmazonS3 s3Client;

    public ImageGenerationController(ImageService imageService, ImageRepository imageRepository, AmazonS3 s3Client) {
        this.imageService = imageService;
        this.imageRepository = imageRepository;
        this.s3Client = s3Client;
    }

    @PostMapping("/image")
    public ResponseEntity<ImageResponse> createImage(@RequestBody @Validated ImageRequest request) {
        log.info("Got request to generate image: {}", request.getReqId());
        ImageFile file = imageService.createImage(request);
        ImageResponse response = new ImageResponse();
        BeanUtils.copyProperties(file, response);
        response.setFileId(file.getId());
        response.setReqId(request.getReqId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/image/{id}/content")
    public void downloadImage(@PathVariable String id, HttpServletResponse response) throws IOException {
        ImageFile file = imageRepository.findById(id).orElseThrow(FileNotFoundException::new);
        String[] parts = file.getFileLocation().split("/", 2); // "bucket/key"
        response.setHeader("Content-Type", "image/png");
        response.setHeader("fileName", file.getFileName());
        try (S3Object object = s3Client.getObject(parts[0], parts[1])) {
            FileCopyUtils.copy(object.getObjectContent(), response.getOutputStream());
        }
    }

    @DeleteMapping("/image/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable String id) {
        log.info("Got request to delete image: {}", id);
        imageService.deleteImage(id);
        return ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
