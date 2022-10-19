package com.antra.report.client.pojo.reponse;

import lombok.Data;

@Data
public class SqsResponse {

    private String fileId;

    private String reqId;

    private String fileLocation;

    private long fileSize;

    private boolean failed;
}

