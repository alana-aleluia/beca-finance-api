package com.becafinance.msusers.adapter.in.web;

import com.becafinance.msusers.application.dto.UserIdentity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @GetMapping("/me")
    public UserIdentity me(@AuthenticationPrincipal UserIdentity identity) { return identity; }
}
