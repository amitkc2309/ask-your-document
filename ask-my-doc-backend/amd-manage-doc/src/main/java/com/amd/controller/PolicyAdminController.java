package com.amd.controller;

import com.amd.security.KeycloakPolicyCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/policy")
@RequiredArgsConstructor
public class PolicyAdminController {

    private final KeycloakPolicyCacheService cacheService;

    @PostMapping("/refresh")
    public void refreshPolicies() {
        cacheService.incrementPolicyVersion();
    }
}
