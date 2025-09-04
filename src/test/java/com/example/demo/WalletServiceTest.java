package com.example.demo;

import com.example.demo.dto.WalletDto;
import com.example.demo.entity.Wallet;
import com.example.demo.exceptions.InsufficientFundsException;
import com.example.demo.exceptions.WalletNotFoundException;
import com.example.demo.mapper.WalletMapper;
import com.example.demo.repository.WalletRepository;
import com.example.demo.service.WalletService;
import com.example.demo.utils.enums.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {
    @Mock
    private WalletMapper walletMapper;

    @Mock
    private RetryTemplate retryTemplate;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private UUID walletId;
    private Wallet wallet;
    private WalletDto walletDtoDeposit;
    private WalletDto walletDtoWithdraw;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        wallet = new Wallet(walletId, 100.0);
        walletDtoDeposit = new WalletDto(walletId, OperationType.DEPOSIT, 50.0);
        walletDtoWithdraw = new WalletDto(walletId, OperationType.WITHDRAW, 30.0);
    }

    @Test
    void testChangeBalance_Deposit_Success() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(walletMapper.mapToDto(any(Wallet.class), any(OperationType.class)))
                .thenReturn(new WalletDto(walletId, OperationType.DEPOSIT, 150.0));

        when(retryTemplate.execute(any(RetryCallback.class), any(RecoveryCallback.class))).thenAnswer(invocation -> {
            RetryCallback<WalletDto, Throwable> callback = invocation.getArgument(0);

            WalletDto resultDto = callback.doWithRetry(mock(RetryContext.class));

            return resultDto;
        });

        WalletDto result = walletService.changeBalance(walletDtoDeposit);

        assertNotNull(result);
        assertEquals(150, wallet.getAmount());
        assertEquals(walletId, result.walletId());
        assertEquals(150.0, result.amount());
    }

    @Test
    void testChangeBalance_Withdraw_Success() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(walletMapper.mapToDto(any(Wallet.class), any(OperationType.class)))
                .thenReturn(new WalletDto(walletId, OperationType.WITHDRAW, 70.0));

        when(retryTemplate.execute(any(RetryCallback.class), any(RecoveryCallback.class))).thenAnswer(invocation -> {
            RetryCallback<WalletDto, Throwable> callback = invocation.getArgument(0);

            WalletDto updatedWallet = callback.doWithRetry(mock(RetryContext.class));

            return updatedWallet;
        });

        WalletDto result = walletService.changeBalance(walletDtoWithdraw);

        assertNotNull(result);
        assertEquals(70, wallet.getAmount());
        assertEquals(walletId, result.walletId());
        assertEquals(70.0, result.amount());
    }


    @Test
    void testChangeBalance_WalletNotFound() {
        when(walletRepository.findById(walletDtoDeposit.walletId())).thenReturn(Optional.empty());

        when(retryTemplate.execute(any(RetryCallback.class), any(RecoveryCallback.class))).thenAnswer(invocation -> {
            RetryCallback<Object, Throwable> callback = invocation.getArgument(0);

            return callback.doWithRetry(mock(RetryContext.class));
        });

        assertThrows(RuntimeException.class, () -> walletService.changeBalance(walletDtoDeposit));
        verify(walletRepository).findById(walletDtoDeposit.walletId());
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(retryTemplate).execute(any(RetryCallback.class), any(RecoveryCallback.class));
    }


    @Test
    void testFindById_Success() {
        when(walletRepository.findBalanceById(walletId)).thenReturn(Optional.of(wallet.getAmount()));
        Double balance = walletService.findById(walletId);

        verify(walletRepository).findBalanceById(walletId);
        assertEquals(wallet.getAmount().doubleValue(), balance);
    }

    @Test
    void testFindById_WalletNotFound() {
        when(walletRepository.findBalanceById(walletId)).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class, () -> walletService.findById(walletId));
    }

    @Test
    void testChangeBalance_InsufficientFunds() {
        OperationType operationType = OperationType.WITHDRAW;
        double initialBalance = 100.0;
        double withdrawAmount = 200.0;

        Wallet wallet = mock(Wallet.class);
        when(wallet.getAmount()).thenReturn(initialBalance);

        WalletDto walletDto = getWalletDto(walletId, withdrawAmount, operationType);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        when(retryTemplate.execute(any(RetryCallback.class), any(RecoveryCallback.class))).thenAnswer(invocation -> {
            RetryCallback<Object, Throwable> callback = invocation.getArgument(0);

            return callback.doWithRetry(mock(RetryContext.class));
        });

        assertThrows(InsufficientFundsException.class, () -> walletService.changeBalance(walletDto),
                "Expected InsufficientFundsException when withdrawing more than available balance");

        verify(walletRepository).findById(walletId);
        verify(wallet, never()).setAmount(anyDouble());
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(retryTemplate).execute(any(RetryCallback.class), any(RecoveryCallback.class));
    }

    @Test
    void testChangeBalance_Success_Deposit() {
        OperationType operationType = OperationType.DEPOSIT;
        double initialBalance = 100.0;
        double depositAmount = 50.0;
        double expectedNewBalance = initialBalance + depositAmount;

        Wallet wallet = new Wallet(initialBalance);
        Wallet updatedWallet = new Wallet(expectedNewBalance);
        WalletDto expectedDto = new WalletDto(walletId, operationType, expectedNewBalance);

        WalletDto walletDto = getWalletDto(walletId, depositAmount, operationType);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        when(walletRepository.save(any(Wallet.class))).thenReturn(updatedWallet);

        when(walletMapper.mapToDto(updatedWallet, operationType)).thenReturn(expectedDto);

        when(retryTemplate.execute(any(RetryCallback.class), any(RecoveryCallback.class))).thenAnswer(invocation -> {
            RetryCallback<Object, Throwable> callback = invocation.getArgument(0);
            return callback.doWithRetry(mock(RetryContext.class));
        });

        WalletDto result = walletService.changeBalance(walletDto);

        assertNotNull(result);
        assertEquals(expectedDto.walletId(), result.walletId());
        assertEquals(expectedDto.amount(), result.amount());
        assertEquals(expectedDto.operationType(), result.operationType());

        verify(walletRepository).findById(walletId);
        verify(walletRepository).save(any(Wallet.class));
        verify(walletMapper).mapToDto(updatedWallet, operationType);
        verify(retryTemplate).execute(any(RetryCallback.class), any(RecoveryCallback.class));
    }

    private WalletDto getWalletDto(UUID walletId, Double amount, OperationType operationType) {
        return new WalletDto(walletId, operationType, amount);
    }
}
