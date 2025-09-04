package com.example.demo.controller;

import com.example.demo.dto.WalletDto;
import com.example.demo.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/wallets/{id}")
    public Double getBalance(@PathVariable @NotNull UUID id) {
        return walletService.findById(id);
    }

    @PostMapping(path = "/wallet", produces = "application/json")
    public WalletDto changeBalance(@RequestBody @Valid WalletDto walletDto) {
        return walletService.changeBalance(walletDto);
    }

}

