package com.app.be.repository.specification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductSpecificationTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("escapeLikePattern returns empty string when input is null, empty, or whitespace only")
    void shouldReturnEmptyStringForBlankInputs(String input) {
        String result = ProductSpecification.escapeLikePattern(input);
        assertEquals("", result);
    }

    @Test
    @DisplayName("escapeLikePattern does not modify strings without special LIKE characters")
    void shouldNotModifyNormalString() {
        String input = "Whey Protein Isolate";
        String result = ProductSpecification.escapeLikePattern(input);
        assertEquals("Whey Protein Isolate", result);
    }

    @Test
    @DisplayName("escapeLikePattern escapes percent signs")
    void shouldEscapePercentSign() {
        String input = "100% Whey";
        String result = ProductSpecification.escapeLikePattern(input);
        assertEquals("100\\% Whey", result);
    }

    @Test
    @DisplayName("escapeLikePattern escapes underscore characters")
    void shouldEscapeUnderscore() {
        String input = "product_name_sample";
        String result = ProductSpecification.escapeLikePattern(input);
        assertEquals("product\\_name\\_sample", result);
    }

    @Test
    @DisplayName("escapeLikePattern escapes backslash characters")
    void shouldEscapeBackslash() {
        String input = "folder\\item";
        String result = ProductSpecification.escapeLikePattern(input);
        assertEquals("folder\\\\item", result);
    }

    @Test
    @DisplayName("escapeLikePattern escapes mixed special characters correctly")
    void shouldEscapeMixedSpecialCharacters() {
        String input = "100%_pure\\protein";
        String result = ProductSpecification.escapeLikePattern(input);
        assertEquals("100\\%\\_pure\\\\protein", result);
    }

    @Test
    @DisplayName("escapeLikePattern truncates input exceeding MAX_KEYWORD_LENGTH before escaping")
    void shouldTruncateExcessivelyLongKeyword() {
        String longInput = "a".repeat(120);
        String result = ProductSpecification.escapeLikePattern(longInput);
        assertEquals(ProductSpecification.MAX_KEYWORD_LENGTH, result.length());
        assertEquals("a".repeat(100), result);
    }
}
