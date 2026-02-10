package com.br.userservice.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public final class Email {

    // Regex simples para email (suficiente para validacao de dominio comum)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MAX_LENGTH = 254;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String value;

    protected Email() {
        // Construtor protegido para JPA
    }

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Email nao pode ser nulo ou vazio");
        }

        String normalized = raw.trim().toLowerCase();

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Email excede o tamanho maximo permitido");
        }

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email invalido");
        }

        return new Email(normalized);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Email)) {
            return false;
        }
        Email email = (Email) o;
        return Objects.equals(value, email.value);
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
