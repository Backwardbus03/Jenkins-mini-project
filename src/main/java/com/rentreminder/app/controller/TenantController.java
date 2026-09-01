package com.rentreminder.app.controller;

import com.rentreminder.app.model.Tenant;
import com.rentreminder.app.service.TenantService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public String listTenants(Model model) {
        model.addAttribute("tenants", tenantService.getAllTenants());
        return "tenants/list";
    }

    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("tenant", new Tenant());
        return "tenants/form";
    }

    @PostMapping
    public String saveTenant(@Valid @ModelAttribute("tenant") Tenant tenant, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "tenants/form";
        }
        tenantService.saveTenant(tenant);
        return "redirect:/tenants";
    }
}
