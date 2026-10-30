package org.example.io.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ContactBookDto(@JsonProperty("schema_version") Integer schemaVersion,
                             ContactDto[] contacts) {
}
