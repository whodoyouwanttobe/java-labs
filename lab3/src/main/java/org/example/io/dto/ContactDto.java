package org.example.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContactDto(
        @JsonProperty("type") String type,
        @JsonProperty("name") String name,
        @JsonProperty("phone") String phone,
        @JsonProperty("email") String email,
        @JsonProperty("organization") String organization,
        @JsonProperty("position") String position,
        @JsonProperty("internal_number") String internalNumber) {
}
