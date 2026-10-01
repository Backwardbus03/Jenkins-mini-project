package com.rentreminder.app.controller;

import com.rentreminder.app.model.Tenant;
import com.rentreminder.app.model.TenantInvitation;
import com.rentreminder.app.service.TenantInvitationService;
import com.rentreminder.app.service.TenantService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final TenantInvitationService invitationService;

    public TenantController(TenantService tenantService,
                            TenantInvitationService invitationService) {
        this.tenantService = tenantService;
        this.invitationService = invitationService;
    }

    @GetMapping({"", "/"})
    public String listTenants(Model model) {
        List<Tenant> allTenants = tenantService.getAllTenants();
        Map<Long, TenantInvitation> invitationsMap = invitationService.getLatestInvitationsByTenant();

        List<Tenant> activeTenants = new ArrayList<>();
        List<Tenant> expiredTenants = new ArrayList<>();

        for (Tenant t : allTenants) {
            TenantInvitation inv = invitationsMap.get(t.getId());
            if (inv != null && inv.isExpired()) {
                expiredTenants.add(t);
            } else {
                activeTenants.add(t);
            }
        }

        model.addAttribute("tenants", activeTenants);
        model.addAttribute("activeTenants", activeTenants);
        model.addAttribute("expiredTenants", expiredTenants);
        model.addAttribute("invitationsMap", invitationsMap);
        return "tenants/list";
    }

    @GetMapping({"/new", "/form", "/add"})
    public String showAddForm(Model model) {
        model.addAttribute("tenant", new Tenant());
        return "tenants/form";
    }

    @PostMapping({"", "/"})
    public String saveTenant(@Valid @ModelAttribute("tenant") Tenant tenant,
                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "tenants/form";
        }
        Tenant saved = tenantService.saveTenant(tenant);

        // Automatically create portal invitation using the tenant's email address
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            invitationService.createInvitation(saved, saved.getEmail().trim());
        }
        return "redirect:/tenants";
    }
}

