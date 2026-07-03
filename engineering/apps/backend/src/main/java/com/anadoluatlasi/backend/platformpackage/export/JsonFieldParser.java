package com.anadoluatlasi.backend.platformpackage.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * FP-005: JSON string alanlarını (hashtagsJson, tagsJson, vb.) Java List'e parse eden utility.
 *
 * <p>Parse hatası durumunda hata fırlatmaz — orijinal string'i tek elemanlı liste olarak döner.
 * Bu yaklaşım, bozuk JSON alanlarının export'u engellemesini önler.</p>
 */
final class JsonFieldParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private JsonFieldParser() {
        // Utility class
    }

    /**
     * JSON array string'ini Java listesine parse eder.
     *
     * @param jsonArrayString JSON array string (ör. '["#tag1","#tag2"]')
     * @return Parse edilmiş liste, null/boş/bozuk durumda boş liste
     */
    static List<String> parseJsonArray(String jsonArrayString) {
        if (jsonArrayString == null || jsonArrayString.isBlank()) {
            return List.of();
        }
        try {
            List<String> result = OBJECT_MAPPER.readValue(jsonArrayString, STRING_LIST_TYPE);
            return result != null ? result : List.of();
        } catch (Exception e) {
            // Parse hatası — orijinal string'i tek elemanlı liste olarak döndür
            return List.of(jsonArrayString);
        }
    }

    /**
     * JSON array'i insan-okunabilir stringe çevirir (virgülle ayrılmış).
     */
    static String joinParsed(String jsonArrayString) {
        List<String> items = parseJsonArray(jsonArrayString);
        return items.isEmpty() ? "" : String.join(", ", items);
    }

    /**
     * JSON array'i satır satır stringe çevirir.
     */
    static String joinParsedLines(String jsonArrayString) {
        List<String> items = parseJsonArray(jsonArrayString);
        return items.isEmpty() ? "" : String.join("\n", items);
    }
}
