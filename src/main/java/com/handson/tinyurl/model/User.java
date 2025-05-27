package com.handson.tinyurl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    // Remove @Indexed completely
    private String name;

    private int allUrlClicks;

    private Map<String, ShortUrl> shorts;

    public User() {
        this.shorts = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAllUrlClicks() {
        return allUrlClicks;
    }

    public void setAllUrlClicks(int allUrlClicks) {
        this.allUrlClicks = allUrlClicks;
    }

    public Map<String, ShortUrl> getShorts() {
        return shorts;
    }

    public void setShorts(Map<String, ShortUrl> shorts) {
        this.shorts = shorts;
    }

    // Builder pattern (unchanged)
    public static class UserBuilder {
        private String name;
        private int allUrlClicks;
        private Map<String, ShortUrl> shorts = new HashMap<>();

        public static UserBuilder anUser() {
            return new UserBuilder();
        }

        public UserBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder withAllUrlClicks(int allUrlClicks) {
            this.allUrlClicks = allUrlClicks;
            return this;
        }

        public UserBuilder withShorts(Map<String, ShortUrl> shorts) {
            this.shorts = shorts;
            return this;
        }

        public User build() {
            User user = new User();
            user.setName(name);
            user.setAllUrlClicks(allUrlClicks);
            user.setShorts(shorts);
            return user;
        }
    }
}