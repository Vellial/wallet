package com.example.demo.service;

import com.example.demo.dto.WalletDto;
import com.example.demo.entity.Wallet;
import com.example.demo.exceptions.InsufficientFundsException;
import com.example.demo.exceptions.WalletNotFoundException;
import com.example.demo.mapper.WalletMapper;
import com.example.demo.repository.WalletRepository;
import com.example.demo.utils.enums.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.example.demo.utils.enums.OperationType.WITHDRAW;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final RetryTemplate retryTemplate;
    private final WalletMapper walletMapper;

    public Double findById(UUID id) {
        return walletRepository.findBalanceById(id).orElseThrow(() ->
                new WalletNotFoundException(HttpStatus.NOT_FOUND, String.format("Wallet with id %s not found", id)));
    }

    @Transactional
    public WalletDto changeBalance(WalletDto walletDto) {
        return retryTemplate.execute(retryContext -> {
            Wallet wallet = walletRepository.findById(walletDto.walletId())
                    .orElseThrow(() -> new WalletNotFoundException(
                            HttpStatus.NOT_FOUND,
                            String.format("Wallet with id %s not found", walletDto.walletId())
                    ));

            if (walletDto.operationType() == WITHDRAW &&
                    wallet.getAmount().compareTo(walletDto.amount()) < 0) {
                throw new InsufficientFundsException(wallet.getAmount(), walletDto.amount());
            }

            updateBalance(wallet, walletDto.operationType(), walletDto.amount());
            Wallet savedWallet = walletRepository.save(wallet);
            return walletMapper.mapToDto(savedWallet, walletDto.operationType());
        }, recoveryContext -> {
            throw (RuntimeException) recoveryContext.getLastThrowable();
        });
    }

    private void updateBalance(Wallet wallet, OperationType op, double amount) {
        switch (op) {
            case DEPOSIT:
                wallet.setAmount(wallet.getAmount() + amount);
                break;
            case WITHDRAW:
                wallet.setAmount(wallet.getAmount() - amount);
                break;
        }
    }

}
