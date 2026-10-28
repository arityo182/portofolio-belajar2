package com.koperasi.notification.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record MarkReadResponse(UUID id, @JsonProperty("isRead") boolean isRead) {
}
