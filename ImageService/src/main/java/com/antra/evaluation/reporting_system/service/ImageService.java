package com.antra.evaluation.reporting_system.service;

import com.antra.evaluation.reporting_system.pojo.api.ImageRequest;
import com.antra.evaluation.reporting_system.pojo.report.ImageFile;

public interface ImageService {
    ImageFile createImage(ImageRequest request);

    void deleteImage(String id);
}
