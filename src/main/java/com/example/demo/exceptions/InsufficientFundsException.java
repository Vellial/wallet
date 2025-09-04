package com.example.demo.exceptions;

import lombok.Getter;

@Getter
public class InsufficientFundsException extends RuntimeException {
    private final double availableBalance;
    private final double requestedAmount;

    public InsufficientFundsException(double availableBalance, double requestedAmount) {
        super("Недостаточно средств на кошельке для выполнения операции. Доступно: " + availableBalance + ", Запрошено: " + requestedAmount + ".");
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }

}
