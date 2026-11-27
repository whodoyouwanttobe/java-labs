package org.example.io.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContactFileDto {
    @JsonProperty("schema_version")
    public int schemaVersion;
    public ContactDto[] contacts;
}

