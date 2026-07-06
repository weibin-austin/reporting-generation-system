package com.antra.report.client.pojo.reponse;

import lombok.Data;

@Data
public class ImageResponse {

    private String reqId;

    private String fileId;

    private String fileLocation;

    private long fileSize;

    private boolean failed;
}
