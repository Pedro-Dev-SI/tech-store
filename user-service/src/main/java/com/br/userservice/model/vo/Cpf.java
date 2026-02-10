package com.br.userservice.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public final class Cpf {

    @Column(name = "cpf", nullable = false, unique = true, length = 11)
    private String value;

    protected Cpf() {
        // Construtor protegido para JPA
    }

    private Cpf(String value) {
        this.value = value;
    }

    public static Cpf of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("CPF nao pode ser nulo ou vazio");
        }

        String digits = raw.replaceAll("\\D", "");

        if (digits.length() != 11) {
            throw new IllegalArgumentException("CPF deve ter 11 digitos");
        }

        if (allDigitsEqual(digits)) {
            throw new IllegalArgumentException("CPF invalido");
        }

        if (!isValidCpf(digits)) {
            throw new IllegalArgumentException("CPF invalido");
        }

        return new Cpf(digits);
    }

    public String getValue() {
        return value;
    }

    private static boolean allDigitsEqual(String digits) {
        char first = digits.charAt(0);
        for (int i = 1; i < digits.length(); i++) {
            if (digits.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidCpf(String digits) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * (10 - i);
        }
        int firstDigit = 11 - (sum % 11);
        firstDigit = (firstDigit >= 10) ? 0 : firstDigit;

        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (digits.charAt(i) - '0') * (11 - i);
        }
        int secondDigit = 11 - (sum % 11);
        secondDigit = (secondDigit >= 10) ? 0 : secondDigit;

        return firstDigit == (digits.charAt(9) - '0')
            && secondDigit == (digits.charAt(10) - '0');
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cpf)) {
            return false;
        }
        Cpf cpf = (Cpf) o;
        return Objects.equals(value, cpf.value);
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
