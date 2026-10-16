package com.app.be.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s]");
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9-]");
    private static final Pattern MULTIPLE_HYPHENS_PATTERN = Pattern.compile("-+");

    private SlugUtils() {
        // Private constructor for utility class
    }

    public static String generateSlug(String name, String customSlug) {
        if (customSlug != null && !customSlug.isBlank()) {
            return toSlug(customSlug);
        }
        return toSlug(name);
    }

    public static String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String noWhitespace = WHITESPACE_PATTERN.matcher(input.trim().toLowerCase()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        slug = slug.replaceAll("[đĐ]", "d");
        slug = NON_ALPHANUMERIC_PATTERN.matcher(slug).replaceAll("");
        slug = MULTIPLE_HYPHENS_PATTERN.matcher(slug).replaceAll("-");
        return slug.replaceAll("^-|-$", "");
    }
}
