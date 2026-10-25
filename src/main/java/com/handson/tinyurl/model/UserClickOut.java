
package com.handson.tinyurl.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class UserClickOut {

    private String userName;
    private Date clickTime;
    private String tiny;
    private String longUrl;

    public String getUserName() {
        return userName;
    }

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Asia/Jerusalem"
    )
    public Date getClickTime() {
        return clickTime;
    }

    public String getTiny() {
        return tiny;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public static UserClickOut of(UserClick userClick) {
        UserClickOut result = new UserClickOut();

        result.userName =
                userClick.getUserClickKey().getUserName();

        result.clickTime =
                userClick.getUserClickKey().getClickTime();

        result.tiny = userClick.getTiny();
        result.longUrl = userClick.getLongUrl();

        return result;
    }
}
