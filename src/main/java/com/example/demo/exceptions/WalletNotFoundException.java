package com.example.demo.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class WalletNotFoundException extends RuntimeException {
    private HttpStatus status;

    public WalletNotFoundException(HttpStatus status, String message, Object... arguments) {
        super(String.format(message, arguments));
        this.status = status;
    }
}
