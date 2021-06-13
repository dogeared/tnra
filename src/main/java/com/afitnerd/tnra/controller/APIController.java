package com.afitnerd.tnra.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1")
public class APIController {

    @GetMapping("/me")
    Principal me(Principal me) {
        return me;
    }
}
