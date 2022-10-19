package com.antra.report.client.pojo.reponse;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
public class GeneralResponse {

    private HttpStatus statusCode = HttpStatus.OK;

    private Object data;

    public GeneralResponse(Object data) {
        this.data = data;
    }
}
