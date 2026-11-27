package org.example.server.config;

import org.example.store.ContactStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StoreConfig {

    @Bean
    public ContactStore contactStore() {
        return new ContactStore();
    }
}
