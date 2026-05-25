package com.helper;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads JSON test-data files from the classpath and converts them to Object[][]
 * for use with TestNG {@code @DataProvider}.
 *
 * Supported JSON shapes:
 * <pre>
 * // Flat array
 * [ { "keyword": "SON MOI", "page": 1 }, ... ]
 *
 * // Wrapped array
 * { "testCases": [ { "keyword": "SON MOI" }, ... ] }
 * </pre>
 *
 * Usage in DataProviders:
 * <pre>
 * {@literal @}DataProvider(name = "searchData")
 * public Object[][] searchDataProvider() {
 *     return GsonDataProvider.loadFromResource(
 *         "jsonData/SearchData.json",
 *         new String[]{"keyword", "page", "size", "expectedMinProducts", "description"}
 *     );
 * }
 * </pre>
 */
public class GsonDataProvider {

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Loads a flat JSON array from the classpath and returns Object[][] mapped
     * by the specified field names.
     */
    public static Object[][] loadFromResource(String resourcePath, String[] fields) {
        List<Map<String, Object>> data = readJsonArray(resourcePath);
        LogUtils.info("[GsonDataProvider] " + data.size() + " row(s) loaded from: " + resourcePath);
        return toObjectArray(data, fields);
    }

    /**
     * Loads a nested JSON array (wrapped inside a root object key) and returns Object[][].
     */
    public static Object[][] loadFromResource(String resourcePath, String arrayKey, String[] fields) {
        List<Map<String, Object>> data = readJsonArray(resourcePath, arrayKey);
        LogUtils.info("[GsonDataProvider] " + data.size() + " row(s) loaded from: "
                + resourcePath + " -> key: " + arrayKey);
        return toObjectArray(data, fields);
    }

    /** Reads the entire file as a {@code JsonObject}. Useful for flat config files. */
    public static JsonObject readJsonObject(String resourcePath) {
        try (InputStreamReader reader = openReader(resourcePath)) {
            if (reader == null) return new JsonObject();
            return GSON.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            LogUtils.error("[GsonDataProvider] Failed to read JsonObject: " + e.getMessage());
            return new JsonObject();
        }
    }

    /** Reads a single String field from a flat JSON object file. */
    public static String getStringField(String resourcePath, String fieldName) {
        JsonElement el = readJsonObject(resourcePath).get(fieldName);
        return (el != null && !el.isJsonNull()) ? el.getAsString() : "";
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static List<Map<String, Object>> readJsonArray(String resourcePath) {
        try (InputStreamReader reader = openReader(resourcePath)) {
            if (reader == null) return new ArrayList<>();
            Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> result = GSON.fromJson(reader, type);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            LogUtils.error("[GsonDataProvider] Failed to read JSON array from: " + resourcePath + " — " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static List<Map<String, Object>> readJsonArray(String resourcePath, String arrayKey) {
        try (InputStreamReader reader = openReader(resourcePath)) {
            if (reader == null) return new ArrayList<>();
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (!root.has(arrayKey)) {
                LogUtils.error("[GsonDataProvider] Key '" + arrayKey + "' not found in: " + resourcePath);
                return new ArrayList<>();
            }
            Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
            return GSON.fromJson(root.get(arrayKey), type);
        } catch (Exception e) {
            LogUtils.error("[GsonDataProvider] Failed to read array key '" + arrayKey + "': " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static Object[][] toObjectArray(List<Map<String, Object>> data, String[] fields) {
        if (data == null || data.isEmpty()) {
            LogUtils.warn("[GsonDataProvider] No data to convert");
            return new Object[0][0];
        }
        Object[][] result = new Object[data.size()][fields.length];
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            for (int j = 0; j < fields.length; j++) {
                result[i][j] = normalise(row.get(fields[j]));
            }
        }
        return result;
    }

    /**
     * Normalises Gson's default numeric type (Double) to Integer when the value
     * is a whole number — avoids "1.0" instead of "1" in test output.
     */
    private static Object normalise(Object value) {
        if (value instanceof Double) {
            double d = (Double) value;
            if (d == Math.floor(d) && !Double.isInfinite(d)) return (int) d;
        }
        return value;
    }

    private static InputStreamReader openReader(String resourcePath) {
        InputStream is = GsonDataProvider.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            LogUtils.error("[GsonDataProvider] Resource not found: " + resourcePath);
            return null;
        }
        return new InputStreamReader(is, StandardCharsets.UTF_8);
    }
}
