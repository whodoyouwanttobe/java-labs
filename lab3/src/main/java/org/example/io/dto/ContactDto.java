package org.example.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContactDto(String type,
                         String name,
                         String phone,
                         String email,
                         String organization,
                         String position,
                         @JsonProperty("internal_number") String internalNumber) {
}
