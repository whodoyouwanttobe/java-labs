package org.example.server;

import org.example.model.Contact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactService service;

    @Test
    void contextStartsAndReturnsFilteredList() throws Exception {
        service.replaceAll(new Contact[] {
                new Contact("Анна", "+70000000001", "a@example.com", "НГТУ"),
                new Contact("Борис", "+70000000002", "b@example.com", "НГТУ")
        });

        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/contacts").param("prefix", "Ан"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void missingContactReturnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/contacts/{id}", "Нет такого"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTACT_NOT_FOUND"));
    }
}
