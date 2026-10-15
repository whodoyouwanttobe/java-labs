package org.example.io.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"schema_version", "contacts"})
public record ContactBookDto(
        @JsonProperty("schema_version") Integer schemaVersion,
        @JsonProperty("contacts") ContactDto[] contacts) {
}
