package org.example.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactDto {
    public String type;
    public String name;
    public String phone;
    public String email;
    public String organization;
    public String position;
    public String internalNumber;
    public String updatedAt;
    public String note;
}

