package com.antra.report.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestSendEmail {

    @Autowired
    private QueueMessagingTemplate queueMessagingTemplate;


    @Test
    public void testSendingEmail() {
        Map<String, String> message = new HashMap<>();
        message.put("to", "austin.sun89@gmail.com");
        message.put("from", "austin.sun89@gmail.com");
        message.put("subject", "Test Email");
        message.put("body", "I did it");
        message.put("token", "12345");
        queueMessagingTemplate.convertAndSend("Email_Queue", message);
    }
}
