package com.becafinance.mstransactions.adapter.in.web;
import com.becafinance.mstransactions.application.dto.*;
import com.becafinance.mstransactions.application.port.in.ManageTransactionsPort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/account")
public class AccountController {
    private final ManageTransactionsPort management;
    public AccountController(ManageTransactionsPort management) { this.management = management; }
    @GetMapping("/balance") public AccountBalance balance(@AuthenticationPrincipal UserIdentity user) { return management.balance(user.id()); }
}
