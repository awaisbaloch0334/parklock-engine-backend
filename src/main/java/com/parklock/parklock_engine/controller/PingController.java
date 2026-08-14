package com.parklock.parklock_engine.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/")
    public String ping() {
        return "PONG! Parklock Engine is live and serving the latest code!";
    }
}