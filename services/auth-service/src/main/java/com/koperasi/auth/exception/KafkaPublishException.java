package com.koperasi.auth.exception;

import org.springframework.http.HttpStatus;

public class KafkaPublishException extends TechnicalException {

    public KafkaPublishException(String message, Throwable cause) {
        super(ErrorCode.KAFKA_PUBLISH_ERROR, message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
