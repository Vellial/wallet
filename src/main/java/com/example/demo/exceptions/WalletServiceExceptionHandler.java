package com.example.demo.exceptions;

import com.example.demo.dto.ErrorResponseDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class WalletServiceExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {WalletNotFoundException.class})
    protected ResponseEntity<Object> handleTechServiceException(WalletNotFoundException ex, WebRequest request) {
        ErrorResponseDto errorDto = new ErrorResponseDto(ex.getStatus(), ex.getMessage());
        return handleExceptionInternal(ex, errorDto, new HttpHeaders(), ex.getStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((org.springframework.validation.FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST,
                "Ошибка валидации запроса.",
                request.getDescription(false).replace("uri=", ""),
                errors
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponseDto> handleInsufficientFundsException(
            InsufficientFundsException ex, WebRequest request) {
        Map<String, Double> details = new HashMap<>();
        details.put("availableBalance", ex.getAvailableBalance());
        details.put("requestedAmount", ex.getRequestedAmount());

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                details
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // Обработка синтаксических ошибок JSON
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String errorMessage = "Некорректный формат JSON в теле запроса.";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            errorMessage += " Причина: " + ex.getCause().getMessage();
        }
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST,
                errorMessage,
                request.getDescription(false).replace("uri=", ""),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Общий обработчик для всех остальных необработанных исключений
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAllUncaughtException(
            Exception ex, WebRequest request) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Произошла непредвиденная ошибка сервера.",
                request.getDescription(false).replace("uri=", ""),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
