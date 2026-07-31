package com.sharecart.sharecart;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class JacksonUnknownPropertiesBehaviorTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = Jackson2ObjectMapperBuilder.json().build();
    }

    @Test
    void shouldNotFailOnUnknownProperties() {
        assertFalse(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    void shouldDisableUnknownPropertiesInApplicationProperties() throws IOException {
        try (InputStream inputStream = Objects.requireNonNull(getClass()
                .getClassLoader()
                .getResourceAsStream("application.properties"))) {
            String properties = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(properties.contains("spring.jackson.deserialization.fail-on-unknown-properties=false"));
        }
    }
}
