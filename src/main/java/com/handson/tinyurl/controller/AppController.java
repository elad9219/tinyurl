package com.handson.tinyurl.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.tinyurl.model.NewTinyRequest;
import com.handson.tinyurl.model.User;
import com.handson.tinyurl.model.UserClick;
import com.handson.tinyurl.model.UserClickOut;
import com.handson.tinyurl.repository.UserClickRepository;
import com.handson.tinyurl.repository.UserRepository;
import com.handson.tinyurl.service.Redis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.dao.DuplicateKeyException;
import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.handson.tinyurl.model.User.UserBuilder.anUser;
import static com.handson.tinyurl.model.UserClick.UserClickBuilder.anUserClick;
import static com.handson.tinyurl.model.UserClickKey.UserClickKeyBuilder.anUserClickKey;
import static com.handson.tinyurl.util.Dates.getCurMonth;

@RestController
public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    private static final int MAX_RETRIES = 4;
    private static final int TINY_LENGTH = 6;

    @Autowired
    Redis redis;

    Random random = new Random();

    @Autowired
    ObjectMapper mapper;

    @Value("${base.url}")
    private String baseUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserClickRepository userClickRepository;

    @PostConstruct
    public void init() {
        logger.info("Loaded baseUrl: {}", baseUrl);
    }

    // Normalize URL to ensure it starts with https:// and includes www. if needed
    private String normalizeUrl(String longUrl) {
        logger.debug("Normalizing URL: {}", longUrl);
        try {
            // If URL doesn't start with http:// or https://, add https://
            if (!longUrl.startsWith("http://") && !longUrl.startsWith("https://")) {
                longUrl = "https://" + longUrl;
            }

            // Parse the URL
            URL url = new URL(longUrl);
            String host = url.getHost();
            String path = url.getPath().isEmpty() ? "/" : url.getPath();

            // If host doesn't start with www., try adding it
            if (!host.startsWith("www.")) {
                String withWww = "https://www." + host + path;
                try {
                    new URL(withWww).toURI(); // Validate the URL
                    logger.debug("Normalized URL with www: {}", withWww);
                    return withWww;
                } catch (Exception e) {
                    // If www. doesn't work, return the original normalized URL
                    logger.debug("Failed to add www, returning original: {}", longUrl);
                    return longUrl;
                }
            }

            logger.debug("URL already normalized: {}", longUrl);
            return longUrl;
        } catch (Exception e) {
            // If parsing fails, return the original URL with https://
            logger.warn("Failed to parse URL, adding https://: {}", longUrl);
            return "https://" + longUrl;
        }
    }

    // Create a new user using a query parameter for the name
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public ResponseEntity<String> createUser(@RequestParam String name) {
        logger.info("Attempting to create user: {}", name);
        try {
            // Check if a user with the same name already exists
            if (userRepository.existsByName(name)) {
                logger.warn("User already exists: {}", name);
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
            }
            // Create and save the new user
            User user = anUser().withName(name).build();
            userRepository.save(user);
            logger.info("User created successfully: {}", name);
            return ResponseEntity.ok("User created successfully");
        } catch (DuplicateKeyException e) {
            logger.warn("Duplicate key detected for user: {}", name, e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating user: " + e.getMessage());
        }
    }

    // Get user details by name
    @RequestMapping(value = "/user/{name}", method = RequestMethod.GET)
    public ResponseEntity<User> getUser(@PathVariable String name) {
        logger.info("Fetching user: {}", name);
        try {
            User user = userRepository.findFirstByName(name);
            logger.debug("User found: {}", user != null ? user.getName() : "null");
            if (user == null) {
                logger.warn("User not found: {}", name);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            // Create a copy of the user with non-null shorts
            User responseUser = anUser()
                    .withName(user.getName())
                    .withAllUrlClicks(user.getAllUrlClicks())
                    .withShorts(user.getShorts() != null ? user.getShorts() : new HashMap<>())
                    .build();
            logger.info("User fetched successfully: {}", name);
            return ResponseEntity.ok(responseUser);
        } catch (Exception e) {
            logger.error("Error fetching user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private void incrementMongoField(String userName, String key) {
        logger.debug("Incrementing Mongo field: userName={}, key={}", userName, key);
        try {
            Query query = new Query(Criteria.where("name").is(userName));
            Update update = new Update().inc(key, 1);
            mongoTemplate.updateFirst(query, update, "users");
            logger.debug("Field incremented successfully: userName={}, key={}", userName, key);
        } catch (Exception e) {
            logger.error("Error incrementing Mongo field: userName={}, key={}, error={}", userName, key, e.getMessage(), e);
        }
    }

    private void updateUserShorts(String userName, String tinyCode, String longUrl) {
        logger.debug("Updating shorts for user: {}, tinyCode: {}, longUrl: {}", userName, tinyCode, longUrl);
        try {
            Query query = new Query(Criteria.where("name").is(userName));
            Update update = new Update().set("shorts." + tinyCode + ".longUrl", longUrl);
            mongoTemplate.updateFirst(query, update, "users");
            logger.debug("Shorts updated successfully for user: {}", userName);
        } catch (Exception e) {
            logger.error("Error updating shorts for user: {}, error: {}", userName, e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/tiny", method = RequestMethod.POST)
    public String generate(@RequestBody NewTinyRequest request) throws JsonProcessingException {
        logger.info("Generating tiny URL for: longUrl={}, userName={}", request.getLongUrl(), request.getUserName());
        String longUrl = normalizeUrl(request.getLongUrl());
        // Create a new NewTinyRequest with the normalized URL using reflection
        NewTinyRequest normalizedRequest = new NewTinyRequest();
        try {
            Field longUrlField = NewTinyRequest.class.getDeclaredField("longUrl");
            longUrlField.setAccessible(true);
            longUrlField.set(normalizedRequest, longUrl);

            Field userNameField = NewTinyRequest.class.getDeclaredField("userName");
            userNameField.setAccessible(true);
            userNameField.set(normalizedRequest, request.getUserName());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Failed to set fields in NewTinyRequest: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to set fields in NewTinyRequest", e);
        }

        String tinyCode = generateTinyCode();
        int i = 0;
        while (!redis.set(tinyCode, mapper.writeValueAsString(normalizedRequest)) && i < MAX_RETRIES) {
            tinyCode = generateTinyCode();
            i++;
        }
        if (i == MAX_RETRIES) {
            logger.error("Failed to generate tiny code after {} retries", MAX_RETRIES);
            throw new RuntimeException("SPACE IS FULL");
        }
        // עדכון ה-shorts של המשתמש
        if (normalizedRequest.getUserName() != null) {
            updateUserShorts(normalizedRequest.getUserName(), tinyCode, longUrl);
        }
        // בניית ה-URL
        logger.debug("Base URL: {}", baseUrl);
        logger.debug("Tiny code: {}", tinyCode);
        String tinyUrl = baseUrl + tinyCode + "/";
        logger.info("Tiny URL generated: {}", tinyUrl);
        return tinyUrl;
    }

    @RequestMapping(value = "/{tiny}/", method = RequestMethod.GET)
    public ModelAndView getTiny(@PathVariable String tiny) throws JsonProcessingException {
        logger.info("Fetching tiny URL: {}", tiny);
        if (tiny == null || tiny.trim().isEmpty() || !tiny.matches("[a-zA-Z0-9]+")) {
            logger.warn("Invalid tiny URL: {}", tiny);
            return new ModelAndView("redirect:/error?message=Invalid tiny URL");
        }
        Object tinyRequestStr = redis.get(tiny);
        if (tinyRequestStr == null) {
            logger.warn("Tiny URL not found: {}", tiny);
            return new ModelAndView("redirect:/error?message=Tiny URL not found");
        }
        NewTinyRequest tinyRequest = mapper.readValue(tinyRequestStr.toString(), NewTinyRequest.class);
        if (tinyRequest.getLongUrl() != null) {
            String userName = tinyRequest.getUserName();
            if (userName != null) {
                logger.debug("Recording click for user: {}, tiny: {}", userName, tiny);
                incrementMongoField(userName, "allUrlClicks");
                incrementMongoField(userName, "shorts." + tiny + ".clicks." + getCurMonth());
                userClickRepository.save(anUserClick()
                        .userClickKey(anUserClickKey().withUserName(userName).withClickTime(new Date()).build())
                        .tiny(tiny)
                        .longUrl(tinyRequest.getLongUrl())
                        .build());
                logger.debug("Click recorded successfully for user: {}, tiny: {}", userName, tiny);
            }
            logger.info("Redirecting to: {}", tinyRequest.getLongUrl());
            return new ModelAndView("redirect:" + tinyRequest.getLongUrl());
        } else {
            logger.warn("Invalid tiny URL: {}", tiny);
            return new ModelAndView("redirect:/error?message=Invalid tiny URL");
        }
    }

    @RequestMapping(value = "/user/{name}/clicks", method = RequestMethod.GET)
    public ResponseEntity<List<UserClickOut>> getUserClicks(@PathVariable String name) {
        logger.info("Fetching clicks for user: {}", name);
        try {
            // המרה מ-Iterable ל-List
            Iterable<UserClick> userClicksIterable = userClickRepository.findByUserName(name);
            List<UserClick> userClicks = StreamSupport.stream(userClicksIterable.spliterator(), false)
                    .collect(Collectors.toList());
            List<UserClickOut> userClickOuts = userClicks.stream()
                    .map(UserClickOut::of)
                    .collect(Collectors.toList());
            logger.debug("Found {} clicks for user: {}", userClickOuts.size(), name);
            if (userClickOuts.isEmpty()) {
                logger.info("No clicks found for user: {}", name);
                return ResponseEntity.ok(new ArrayList<>());
            }
            return ResponseEntity.ok(userClickOuts);
        } catch (Exception e) {
            logger.error("Error fetching clicks for user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    private String generateTinyCode() {
        String charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < TINY_LENGTH; i++) {
            res.append(charPool.charAt(random.nextInt(charPool.length())));
        }
        logger.debug("Generated tiny code: {}", res);
        return res.toString();
    }
}