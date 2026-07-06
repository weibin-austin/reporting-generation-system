package com.antra.evaluation.reporting_system.pojo.api;

import lombok.Data;

@Data
public class ImageResponse {

    private String fileId;

    private String reqId;

    private String fileLocation;

    private long fileSize;

    private boolean failed;
}
