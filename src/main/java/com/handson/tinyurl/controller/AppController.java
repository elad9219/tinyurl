
package com.handson.tinyurl.controller;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.json.JsonMapper;

import com.handson.tinyurl.model.NewTinyRequest;
import com.handson.tinyurl.model.User;
import com.handson.tinyurl.model.UserClick;
import com.handson.tinyurl.model.UserClickOut;
import com.handson.tinyurl.repository.UserClickRepository;
import com.handson.tinyurl.repository.UserRepository;
import com.handson.tinyurl.service.Redis;
import com.handson.tinyurl.util.UrlNormalizer;
import com.mongodb.client.result.UpdateResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.handson.tinyurl.model.User.UserBuilder.anUser;
import static com.handson.tinyurl.model.UserClick.UserClickBuilder.anUserClick;
import static com.handson.tinyurl.model.UserClickKey.UserClickKeyBuilder.anUserClickKey;
import static com.handson.tinyurl.util.Dates.getCurMonth;

@RestController
public class AppController {

    private static final Logger logger =
            LoggerFactory.getLogger(AppController.class);

    private static final int MAX_RETRIES = 4;
    private static final int TINY_LENGTH = 6;

    private static final String CHAR_POOL =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private Redis redis;

    @Autowired
    private JsonMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserClickRepository userClickRepository;

    @Value("${base.url}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        logger.info("Loaded baseUrl: {}", baseUrl);
    }

    @PostMapping("/user")
    public ResponseEntity<String> createUser(@RequestParam String name) {

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please enter a username");
        }

        String cleanName = name.trim();

        try {
            if (userRepository.existsByName(cleanName)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("User already exists");
            }

            User user = anUser()
                    .withName(cleanName)
                    .build();

            userRepository.save(user);

            logger.info(
                    "User created successfully: {}",
                    cleanName
            );

            return ResponseEntity.ok(
                    "User created successfully"
            );

        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User already exists");

        } catch (Exception e) {
            logger.error(
                    "Error creating user: {}",
                    cleanName,
                    e
            );

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(
                    "Unable to create user. Please try again."
            );
        }
    }

    // Existing path-based endpoint, preserved for compatibility.
    @GetMapping("/user/{name}")
    public ResponseEntity<User> getUser(
            @PathVariable String name
    ) {

        try {
            User user = userRepository.findFirstByName(name);

            if (user == null) {
                return ResponseEntity.status(
                        HttpStatus.NOT_FOUND
                ).build();
            }

            User responseUser = anUser()
                    .withName(user.getName())
                    .withAllUrlClicks(user.getAllUrlClicks())
                    .withShorts(
                            user.getShorts() != null
                                    ? user.getShorts()
                                    : new HashMap<>()
                    )
                    .build();

            return ResponseEntity.ok(responseUser);

        } catch (Exception e) {
            logger.error(
                    "Error fetching user: {}",
                    name,
                    e
            );

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).build();
        }
    }

    // Query-based lookup supports usernames containing '/'.
    @GetMapping("/user")
    public ResponseEntity<User> getUserByQuery(
            @RequestParam String name
    ) {
        return getUser(name);
    }

    private void incrementMongoField(
            String userName,
            String field
    ) {

        try {
            Query query = new Query(
                    Criteria.where("name").is(userName)
            );

            Update update = new Update().inc(field, 1);

            mongoTemplate.updateFirst(
                    query,
                    update,
                    "users"
            );

        } catch (Exception e) {
            logger.error(
                    "Failed to increment MongoDB field {} for user {}",
                    field,
                    userName,
                    e
            );
        }
    }

    private void removeReservedCode(String tinyCode) {
        try {
            redis.del(tinyCode);

        } catch (Exception e) {
            logger.error(
                    "Failed to remove reserved short code {}",
                    tinyCode,
                    e
            );
        }
    }

    @PostMapping("/tiny")
    public ResponseEntity<String> generate(
            @RequestBody NewTinyRequest request
    ) {

        if (request == null) {
            return ResponseEntity.badRequest()
                    .body("Please provide a URL and username");
        }

        String userName = request.getUserName() == null
                ? ""
                : request.getUserName().trim();

        if (userName.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please enter a username");
        }

        final String normalizedUrl;

        try {
            normalizedUrl = UrlNormalizer.normalize(
                    request.getLongUrl()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }

        try {
            if (!userRepository.existsByName(userName)) {
                return ResponseEntity.status(
                        HttpStatus.NOT_FOUND
                ).body(
                        "User not found. Please create the user first."
                );
            }

        } catch (Exception e) {
            logger.error(
                    "Unable to verify user {}",
                    userName,
                    e
            );

            return ResponseEntity.status(
                    HttpStatus.SERVICE_UNAVAILABLE
            ).body(
                    "Unable to verify the user right now."
            );
        }

        NewTinyRequest normalizedRequest =
                new NewTinyRequest(
                        normalizedUrl,
                        userName
                );

        String json = mapper.writeValueAsString(
                normalizedRequest
        );

        String tinyCode = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {

            String candidate = generateTinyCode();

            if (redis.set(candidate, json)) {
                tinyCode = candidate;
                break;
            }
        }

        if (tinyCode == null) {
            logger.error(
                    "Unable to reserve a short code after {} attempts",
                    MAX_RETRIES
            );

            return ResponseEntity.status(
                    HttpStatus.SERVICE_UNAVAILABLE
            ).body(
                    "Unable to create a short URL right now. Please try again."
            );
        }

        try {
            Query query = new Query(
                    Criteria.where("name").is(userName)
            );

            Update update = new Update()
                    .set(
                            "shorts." + tinyCode + ".longUrl",
                            normalizedUrl
                    )
                    .set(
                            "shorts." + tinyCode + ".clicks",
                            Collections.emptyMap()
                    );

            UpdateResult result = mongoTemplate.updateFirst(
                    query,
                    update,
                    "users"
            );

            if (result.getMatchedCount() == 0) {
                removeReservedCode(tinyCode);

                return ResponseEntity.status(
                        HttpStatus.NOT_FOUND
                ).body(
                        "User not found. Please create the user first."
                );
            }

        } catch (Exception e) {
            logger.error(
                    "Unable to save link metadata for user {}",
                    userName,
                    e
            );

            removeReservedCode(tinyCode);

            return ResponseEntity.status(
                    HttpStatus.SERVICE_UNAVAILABLE
            ).body(
                    "Unable to save the short URL. Please try again."
            );
        }

        String prefix = baseUrl.endsWith("/")
                ? baseUrl
                : baseUrl + "/";

        String tinyUrl = prefix + tinyCode + "/";

        logger.info(
                "Tiny URL created: {}",
                tinyCode
        );

        return ResponseEntity.ok(tinyUrl);
    }

    @GetMapping("/{tiny}/")
    public ModelAndView getTiny(
            @PathVariable String tiny
    ) {

        if (tiny == null
                || tiny.trim().isEmpty()
                || !tiny.matches("[a-zA-Z0-9]+")) {

            return new ModelAndView(
                    "redirect:/error?message=Invalid tiny URL"
            );
        }

        Object storedValue = redis.get(tiny);

        if (storedValue == null) {
            return new ModelAndView(
                    "redirect:/error?message=Tiny URL not found"
            );
        }

        NewTinyRequest tinyRequest = mapper.readValue(
                storedValue.toString(),
                NewTinyRequest.class
        );

        String longUrl = tinyRequest.getLongUrl();

        if (longUrl == null || longUrl.trim().isEmpty()) {
            return new ModelAndView(
                    "redirect:/error?message=Invalid tiny URL"
            );
        }

        String userName = tinyRequest.getUserName();

        if (userName != null && !userName.trim().isEmpty()) {

            try {
                userClickRepository.save(
                        anUserClick()
                                .userClickKey(
                                        anUserClickKey()
                                                .withUserName(userName)
                                                .withClickTime(new Date())
                                                .build()
                                )
                                .tiny(tiny)
                                .longUrl(longUrl)
                                .build()
                );

                incrementMongoField(
                        userName,
                        "allUrlClicks"
                );

                incrementMongoField(
                        userName,
                        "shorts." + tiny + ".clicks." + getCurMonth()
                );

            } catch (Exception e) {
                logger.error(
                        "Unable to save click analytics for short code {}",
                        tiny,
                        e
                );
            }
        }

        return new ModelAndView(
                "redirect:" + longUrl
        );
    }

    // Existing path-based endpoint for GitHub Actions compatibility.
    @GetMapping("/user/{name}/clicks")
    public ResponseEntity<List<UserClickOut>> getUserClicks(
            @PathVariable String name
    ) {

        try {
            Iterable<UserClick> iterable =
                    userClickRepository.findByUserName(name);

            List<UserClick> userClicks =
                    StreamSupport.stream(
                            iterable.spliterator(),
                            false
                    ).collect(Collectors.toList());

            List<UserClickOut> result = userClicks.stream()
                    .map(UserClickOut::of)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error(
                    "Error fetching click history for user {}",
                    name,
                    e
            );

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(new ArrayList<>());
        }
    }

    // Query-based history lookup supports usernames containing '/'.
    @GetMapping("/user/clicks")
    public ResponseEntity<List<UserClickOut>> getUserClicksByQuery(
            @RequestParam String name
    ) {
        return getUserClicks(name);
    }

    private String generateTinyCode() {

        StringBuilder builder = new StringBuilder(
                TINY_LENGTH
        );

        for (int i = 0; i < TINY_LENGTH; i++) {
            builder.append(
                    CHAR_POOL.charAt(
                            RANDOM.nextInt(
                                    CHAR_POOL.length()
                            )
                    )
            );
        }

        return builder.toString();
    }
}
