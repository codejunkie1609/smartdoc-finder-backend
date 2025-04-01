package com.smartdocfinder.core.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    @GetMapping("/api/")
    public String ping() {
        return "pong!";
    }

}
