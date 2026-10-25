
package com.handson.tinyurl.controller;

import tools.jackson.databind.json.JsonMapper;

import com.handson.tinyurl.model.NewTinyRequest;
import com.handson.tinyurl.model.User;
import com.handson.tinyurl.model.UserClickOut;
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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppControllerTest {

    @Mock
    private Redis redis;

    @Mock
    private JsonMapper mapper;

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
    void rejectsLinkCreationForNonexistentUser() {

        when(userRepository.existsByName("missing"))
                .thenReturn(false);

        ResponseEntity<String> response = controller.generate(
                new NewTinyRequest(
                        "https://example.com",
                        "missing"
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertTrue(
                response.getBody().contains("User not found")
        );

        verifyNoInteractions(redis);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void rejectsLinkCreationWithoutUsername() {

        ResponseEntity<String> response = controller.generate(
                new NewTinyRequest(
                        "https://example.com",
                        " "
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        verifyNoInteractions(redis);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void createsLinkForExistingUser() {

        when(userRepository.existsByName("testuser"))
                .thenReturn(true);

        when(mapper.writeValueAsString(
                any(NewTinyRequest.class)
        )).thenReturn(
                "{\"longUrl\":\"https://example.com\"}"
        );

        when(redis.set(anyString(), anyString()))
                .thenReturn(true);

        when(mongoTemplate.updateFirst(
                any(Query.class),
                any(Update.class),
                eq("users")
        )).thenReturn(
                UpdateResult.acknowledged(
                        1L,
                        1L,
                        null
                )
        );

        ResponseEntity<String> response = controller.generate(
                new NewTinyRequest(
                        "example.com",
                        "testuser"
                )
        );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertTrue(
                response.getBody().startsWith(
                        "http://localhost:8080/"
                )
        );

        verify(mongoTemplate).updateFirst(
                any(Query.class),
                any(Update.class),
                eq("users")
        );
    }

    @Test
    void removesReservedCodeWhenUserUpdateDoesNotMatch() {

        when(userRepository.existsByName("testuser"))
                .thenReturn(true);

        when(mapper.writeValueAsString(
                any(NewTinyRequest.class)
        )).thenReturn("{}");

        when(redis.set(anyString(), anyString()))
                .thenReturn(true);

        when(mongoTemplate.updateFirst(
                any(Query.class),
                any(Update.class),
                eq("users")
        )).thenReturn(
                UpdateResult.acknowledged(
                        0L,
                        0L,
                        null
                )
        );

        ResponseEntity<String> response = controller.generate(
                new NewTinyRequest(
                        "example.com",
                        "testuser"
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        verify(redis).del(anyString());
    }

    @Test
    void removesReservedCodeWhenMongoUpdateFails() {

        when(userRepository.existsByName("testuser"))
                .thenReturn(true);

        when(mapper.writeValueAsString(
                any(NewTinyRequest.class)
        )).thenReturn("{}");

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
                new NewTinyRequest(
                        "example.com",
                        "testuser"
                )
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                response.getStatusCode()
        );

        verify(redis).del(anyString());
    }

    @Test
    void rejectsInvalidUrlBeforeAccessingDatabases() {

        ResponseEntity<String> response = controller.generate(
                new NewTinyRequest(
                        "invalid_domain",
                        "testuser"
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        verifyNoInteractions(redis);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void queryLookupSupportsUsernamesWithSlashes() {

        String name = "legacy/name";

        User user = new User();
        user.setName(name);

        when(userRepository.findFirstByName(name))
                .thenReturn(user);

        ResponseEntity<User> response =
                controller.getUserByQuery(name);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                name,
                response.getBody().getName()
        );

        verify(userRepository).findFirstByName(name);
    }

    @Test
    void queryClickLookupSupportsUsernamesWithSlashes() {

        String name = "legacy/name";

        when(userClickRepository.findByUserName(name))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<UserClickOut>> response =
                controller.getUserClicksByQuery(name);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertTrue(response.getBody().isEmpty());

        verify(userClickRepository).findByUserName(name);
    }
}
