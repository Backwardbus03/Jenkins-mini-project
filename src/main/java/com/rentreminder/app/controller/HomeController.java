package com.rentreminder.app.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Landing page & shared login controller.
 * Authenticated users are smartly routed to their respective home screen.
 */
@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            boolean isOwner = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
            return isOwner ? "redirect:/tenants" : "redirect:/portal/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping({"/login", "/portal/login", "/owner/login"})
    public String loginPage(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            boolean isOwner = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
            return isOwner ? "redirect:/tenants" : "redirect:/portal/dashboard";
        }
        return "login";
    }
}

