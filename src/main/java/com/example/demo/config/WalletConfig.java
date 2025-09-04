package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.sql.SQLTransientException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class WalletConfig {

    @Value("${retry.maxAttempts}")
    private int maxAttempts;

    @Value("${retry.initialIntervalMillis}")
    private long initialIntervalMillis;

    @Value("${retry.maxIntervalMillis}")
    private long maxIntervalMillis;

    @Value("${retry.multiplier}")
    private double multiplier;

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 1. Настройка политики повторных попыток (RetryPolicy)
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(ObjectOptimisticLockingFailureException.class, true);
        retryableExceptions.put(PessimisticLockingFailureException.class, true);
        retryableExceptions.put(CannotAcquireLockException.class, true);

        retryableExceptions.put(SQLTransientException.class, true);
        retryableExceptions.put(DeadlockLoserDataAccessException.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts, retryableExceptions, true);

        retryTemplate.setRetryPolicy(retryPolicy);

        // 2. Настройка политики задержки (BackOffPolicy)
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialIntervalMillis);
        backOffPolicy.setMaxInterval(maxIntervalMillis);
        backOffPolicy.setMultiplier(multiplier);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

}
