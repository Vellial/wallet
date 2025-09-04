package com.example.demo.dto;

import com.example.demo.utils.enums.OperationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record WalletDto (
        @NotNull
        UUID walletId,

        @NotNull
        OperationType operationType,

        @Positive
        Double amount
) {
}
