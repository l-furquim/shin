package com.shin.metadata.model.enums;

public enum TranscodingStatus {
    FAILED("failed"),
    QUEUED("queued"),
    PROCESSING("processing"),
    DONE("done");

    private String value;

    TranscodingStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
