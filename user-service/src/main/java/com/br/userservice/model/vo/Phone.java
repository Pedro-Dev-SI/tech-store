package com.br.userservice.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public final class Phone {

    @Column(name = "phone", nullable = false, length = 11)
    private String value;

    protected Phone() {
        // Construtor protegido para JPA
    }

    private Phone(String value) {
        this.value = value;
    }

    public static Phone of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Telefone nao pode ser nulo ou vazio");
        }

        String digits = raw.replaceAll("\\D", "");

        if (digits.length() != 10 && digits.length() != 11) {
            throw new IllegalArgumentException("Telefone deve ter 10 ou 11 digitos");
        }

        return new Phone(digits);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Phone)) {
            return false;
        }
        Phone phone = (Phone) o;
        return Objects.equals(value, phone.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
