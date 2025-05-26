package com.handson.tinyurl.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping(value = {"/", "/{path:[^\\.]*}"})
    public String redirectToIndex() {
        return "forward:/index.html";
    }
}