package com.handson.tinyurl.model;

public class NewTinyRequest {

    private String longUrl;
    private String userName;

    public NewTinyRequest() {
    }

    public NewTinyRequest(String longUrl, String userName) {
        this.longUrl = longUrl;
        this.userName = userName;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
