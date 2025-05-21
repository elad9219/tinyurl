package com.handson.tinyurl.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handson.tinyurl.model.ApiResponse;
import com.handson.tinyurl.model.NewTinyRequest;
import com.handson.tinyurl.model.User;
import com.handson.tinyurl.model.UserClickOut;
import com.handson.tinyurl.repository.UserClickRepository;
import com.handson.tinyurl.repository.UserRepository;
import com.handson.tinyurl.service.Redis;
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

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.handson.tinyurl.model.User.UserBuilder.anUser;
import static com.handson.tinyurl.model.UserClick.UserClickBuilder.anUserClick;
import static com.handson.tinyurl.model.UserClickKey.UserClickKeyBuilder.anUserClickKey;
import static com.handson.tinyurl.util.Dates.getCurMonth;
import static org.springframework.data.util.StreamUtils.createStreamFromIterator;

@RestController
public class AppController {

    private static final int MAX_RETRIES = 4;
    private static final int TINY_LENGTH = 6;

    @Autowired
    Redis redis;

    Random random = new Random();

    @Autowired
    ObjectMapper om;

    @Value("${base.url}")
    String baseUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserClickRepository userClickRepository;

    // Normalize URL to ensure it starts with https:// and includes www. if needed
    private String normalizeUrl(String longUrl) {
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
                    return withWww;
                } catch (Exception e) {
                    // If www. doesn't work, return the original normalized URL
                    return longUrl;
                }
            }

            return longUrl;
        } catch (Exception e) {
            // If parsing fails, return the original URL with https://
            return "https://" + longUrl;
        }
    }

    // Create a new user using a query parameter for the name
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public ResponseEntity<String> createUser(@RequestParam String name) {
        // Check if a user with the same name already exists
        User existingUser = userRepository.findFirstByName(name);
        if (existingUser != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        }
        // Create and insert the new user
        User user = anUser().withName(name).build();
        userRepository.insert(user);
        return ResponseEntity.ok("User created successfully");
    }

    // Get user details by name
    @RequestMapping(value = "/user/{name}", method = RequestMethod.GET)
    public ResponseEntity<User> getUser(@PathVariable String name) {
        User user = userRepository.findFirstByName(name);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        // Create a copy of the user with non-null shorts
        User responseUser = anUser()
                .withName(user.getName())
                .withAllUrlClicks(user.getAllUrlClicks())
                .withShorts(user.getShorts() != null ? user.getShorts() : new HashMap<>())
                .build();
        return ResponseEntity.ok(responseUser);
    }

    private void incrementMongoField(String userName, String key) {
        Query query = Query.query(Criteria.where("name").is(userName));
        Update update = new Update().inc(key, 1);
        mongoTemplate.updateFirst(query, update, "users");
    }

    @RequestMapping(value = "/tiny", method = RequestMethod.POST)
    public String generate(@RequestBody NewTinyRequest request) throws JsonProcessingException {
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
            throw new RuntimeException("Failed to set fields in NewTinyRequest", e);
        }

        String tinyCode = generateTinyCode();
        int i = 0;
        while (!redis.set(tinyCode, om.writeValueAsString(normalizedRequest)) && i < MAX_RETRIES) {
            tinyCode = generateTinyCode();
            i++;
        }
        if (i == MAX_RETRIES) throw new RuntimeException("SPACE IS FULL");
        return baseUrl + tinyCode + "/";
    }

    @RequestMapping(value = "/{tiny}/", method = RequestMethod.GET)
    public ModelAndView getTiny(@PathVariable String tiny) throws JsonProcessingException {
        Object tinyRequestStr = redis.get(tiny);
        NewTinyRequest tinyRequest = om.readValue(tinyRequestStr.toString(), NewTinyRequest.class);
        if (tinyRequest.getLongUrl() != null) {
            String userName = tinyRequest.getUserName();
            if (userName != null) {
                incrementMongoField(userName, "allUrlClicks");
                incrementMongoField(userName, "shorts." + tiny + ".clicks." + getCurMonth());
                userClickRepository.save(anUserClick().userClickKey(anUserClickKey().withUserName(userName).withClickTime(new Date()).build())
                        .tiny(tiny).longUrl(tinyRequest.getLongUrl()).build());
            }
            return new ModelAndView("redirect:" + tinyRequest.getLongUrl());
        } else {
            throw new RuntimeException(tiny + " not found");
        }
    }

    @RequestMapping(value = "/user/{name}/clicks", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<UserClickOut>> getUserClicks(@PathVariable String name) {
        List<UserClickOut> userClicks = createStreamFromIterator(userClickRepository.findByUserName(name).iterator())
                .map(userClick -> UserClickOut.of(userClick))
                .collect(Collectors.toList());
        if (userClicks.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.empty("No clicks found"));
        }
        return ResponseEntity.ok(ApiResponse.success(userClicks));
    }

    private String generateTinyCode() {
        String charPool = "ABCDEFHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < TINY_LENGTH; i++) {
            res.append(charPool.charAt(random.nextInt(charPool.length())));
        }
        return res.toString();
    }
}