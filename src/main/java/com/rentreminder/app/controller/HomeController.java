package com.rentreminder.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Skeleton landing page controller.
 * Feature controllers (tenant onboarding, rent entry, validation,
 * summary view, receipt download) will be added in Week 9-10
 * as part of MVP feature development.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
