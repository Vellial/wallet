package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Entity
@Builder
@Table(name = "wallet")
@NoArgsConstructor
public class Wallet {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    private Double amount;

    public Wallet(Double amount) {
        this.id = UUID.randomUUID();
        this.amount = amount;
    }

    public Wallet(UUID id, Double amount) {
        this.id = id;
        this.amount = amount;
    }

    // @PrePersist для генерации UUID перед сохранением
    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

}
