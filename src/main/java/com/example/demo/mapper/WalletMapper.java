package com.example.demo.mapper;

import com.example.demo.dto.WalletDto;
import com.example.demo.entity.Wallet;
import com.example.demo.utils.enums.OperationType;
import org.springframework.stereotype.Service;

@Service
public class WalletMapper {
    public WalletDto mapToDto(Wallet wallet, OperationType type) {
        return new WalletDto(
                wallet.getId(),
                type,
                wallet.getAmount()
        );
    }

    public Wallet mapToEntity(WalletDto walletDto) {
        return Wallet.builder()
                .amount(walletDto.amount())
                .build();
    }
}