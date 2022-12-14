package com.antra.evaluation.reporting_system.pojo.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.io.IOException;

@Data
public class PDFSNSRequest {
    @JsonProperty("Message")
    @JsonDeserialize(using = SNSMessageDeserializer.class)
    PDFRequest pdfRequest;

}
class SNSMessageDeserializer extends JsonDeserializer<PDFRequest> {
    @Override
    public PDFRequest deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        String text = p.getText();
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        return mapper.readValue(text, PDFRequest.class);
    }
}
