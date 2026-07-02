package com.antra.report.client.pojo;

public enum EmailType {
    SUCCESS("Hi %NAME%, your report is generated."),
    FAILURE("Hi %NAME%, your report generation failed. Please try again or contact support.");

    public String content;

    EmailType(String content) {
        this.content = content;
    }
}
