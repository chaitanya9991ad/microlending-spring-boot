package com.example.microlending.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // 👈 IMPORT THIS
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. Register the Hibernate module
        // (Solves the "ByteBuddyInterceptor" error)
        objectMapper.registerModule(new Hibernate6Module());

        // 2. ⬇️ THIS IS THE NEW FIX ⬇️
        // Register the Java 8 Time module
        // (Solves the "LocalDateTime" error)
        objectMapper.registerModule(new JavaTimeModule());

        // 3. Disable FAIL_ON_EMPTY_BEANS
        // (Solves the "ByteBuddyInterceptor" error)
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 4. (Best Practice)
        // Tell Jackson to write dates as ISO-8601 strings (e.g., "2025-11-01T20:30:23")
        // This is what your JavaScript needs to parse them correctly.
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}

