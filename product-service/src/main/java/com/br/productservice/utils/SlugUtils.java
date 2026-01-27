package com.br.productservice.utils;

import java.text.Normalizer;

public class SlugUtils {

    /**
     * Generates a URL-friendly slug from a string.
     *
     * @param input Raw text
     * @return Normalized slug
     */
    public static String generateSlug(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input não pode ser nulo ou vazio");
        }

        // 1. Remove acentos
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // 2. Converte para lowercase
        String slug = normalized.toLowerCase();

        // 3. Remove caracteres inválidos (mantém letras, números e espaços)
        slug = slug.replaceAll("[^a-z0-9\\s]", "");

        // 4. Converte espaços múltiplos em hífen
        slug = slug.trim().replaceAll("\\s+", "-");

        // 5. Adiciona sufixo
        return slug;
    }
}
