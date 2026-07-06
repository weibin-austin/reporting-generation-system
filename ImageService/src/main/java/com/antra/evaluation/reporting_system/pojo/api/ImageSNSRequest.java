package com.antra.evaluation.reporting_system.pojo.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.io.IOException;

@Data
public class ImageSNSRequest {
    @JsonProperty("Message")
    @JsonDeserialize(using = SNSMessageDeserializer.class)
    ImageRequest imageRequest;
}

class SNSMessageDeserializer extends JsonDeserializer<ImageRequest> {
    @Override
    public ImageRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        return mapper.readValue(text, ImageRequest.class);
    }
}
