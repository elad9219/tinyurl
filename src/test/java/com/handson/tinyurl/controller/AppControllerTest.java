
package com.handson.tinyurl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.tinyurl.model.NewTinyRequest;
import com.handson.tinyurl.repository.UserClickRepository;
import com.handson.tinyurl.repository.UserRepository;
import com.handson.tinyurl.service.Redis;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppControllerTest {

    @Mock
    private Redis redis;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private UserClickRepository userClickRepository;

    @InjectMocks
    private AppController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                controller,
                "baseUrl",
                "http://localhost:8080/"
        );
    }

    @Test
    void rejectsLinkCreationForNonexistentUser() throws Exception {
        when(userRepository.existsByName("missing"))
                .thenReturn(false);

        ResponseEntity<String> response = controller.generate(
                new NewTinyRequest("https://example.com", "missing")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("User not found"));

        verifyNoInteractions(redis);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void rejectsLinkCreationWithoutUsername() throws Exception {
        ResponseEntity<String> response = controller.generate(
                new NewTinyRequest("https://example.com", " ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verifyNoInteractions(redis);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void createsLinkForExistingUser() throws Exception {
        when(userRepository.existsByName("testuser"))
                .thenReturn(true);

        when(mapper.writeValueAsString(any(NewTinyRequest.class)))
                .thenReturn("{\"longUrl\":\"https://example.com\"}");

        when(redis.set(anyString(), anyString()))
                .thenReturn(true);

        when(mongoTemplate.updateFirst(
                any(Query.class),
                any(Update.class),
                eq("users")
        )).thenReturn(
                UpdateResult.acknowledged(1L, 1L, null)
        );

        ResponseEntity<String> response = controller.generate(
                new NewTinyRequest("example.com", "testuser")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertTrue(
                response.getBody().startsWith("http://localhost:8080/")
        );

        verify(mongoTemplate).updateFirst(
                any(Query.class),
                any(Update.class),
                eq("users")
        );
    }

    @Test
    void removesReservedCodeWhenUserUpdateDoesNotMatch()
            throws Exception {

        when(userRepository.existsByName("testuser"))
                .thenReturn(true);

        when(mapper.writeValueAsString(any(NewTinyRequest.class)))
                .thenReturn("{}");

        when(redis.set(anyString(), anyString()))
                .thenReturn(true);

        when(mongoTemplate.updateFirst(
                any(Query.class),
                any(Update.class),
                eq("users")
        )).thenReturn(
                UpdateResult.acknowledged(0L, 0L, null)
        );

        ResponseEntity<String> response = controller.generate(
                new NewTinyRequest("example.com", "testuser")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(redis).del(anyString());
    }

    @Test
    void removesReservedCodeWhenMongoUpdateFails()
            throws Exception {

        when(userRepository.existsByName("testuser"))
                .thenReturn(true);

        when(mapper.writeValueAsString(any(NewTinyRequest.class)))
                .thenReturn("{}");

        when(redis.set(anyString(), anyString()))
                .thenReturn(true);

        when(mongoTemplate.updateFirst(
                any(Query.class),
                any(Update.class),
                eq("users")
        )).thenThrow(
                new RuntimeException("MongoDB unavailable")
        );

        ResponseEntity<String> response = controller.generate(
                new NewTinyRequest("example.com", "testuser")
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                response.getStatusCode()
        );

        verify(redis).del(anyString());
    }

    @Test
    void rejectsInvalidUrlBeforeAccessingDatabases()
            throws Exception {

        ResponseEntity<String> response = controller.generate(
                new NewTinyRequest("invalid_domain", "testuser")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verifyNoInteractions(redis);
        verifyNoInteractions(mongoTemplate);
    }
}
