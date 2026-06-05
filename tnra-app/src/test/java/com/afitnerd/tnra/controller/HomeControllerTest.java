package com.afitnerd.tnra.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeControllerTest {

    @Test
    void redirectForwardsToRoot() {
        HomeController controller = new HomeController();
        assertEquals("forward:/", controller.redirect());
    }
}
