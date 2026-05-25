package com.helper;

import com.google.gson.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonHelper {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls() // Giữ lại các field có giá trị null nếu cần
            .create();

    /**
     * Đọc toàn bộ nội dung file JSON trả về JsonObject
     */
    public static JsonObject getJsonObject(String filePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            LogUtils.error("Lỗi đọc file JSON tại: " + filePath + " - " + e.getMessage());
            return new JsonObject();
        }
    }

    /**
     * Cập nhật hoặc thêm mới một field (String, Number, Boolean, hoặc JsonElement)
     */
    public static void updateValue(String filePath, String key, Object value) {
        try {
            JsonObject jsonObject = getJsonObject(filePath);

            if (value instanceof String) {
                jsonObject.addProperty(key, (String) value);
            } else if (value instanceof Number) {
                jsonObject.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                jsonObject.addProperty(key, (Boolean) value);
            } else if (value instanceof JsonElement) {
                jsonObject.add(key, (JsonElement) value);
            }

            saveFile(filePath, jsonObject);
        } catch (Exception e) {
            LogUtils.error("Không thể update key '" + key + "' trong file: " + filePath);
        }
    }

    /**
     * Cập nhật giá trị trong Nested Object (Object lồng nhau)
     * Ví dụ: updateNestedValue(file, "response.data.name", "New Name")
     */
    public static void updateNestedValue(String filePath, String compositeKey, String newValue) {
        try {
            JsonObject root = getJsonObject(filePath);
            String[] keys = compositeKey.split("\\.");
            JsonObject current = root;

            // Duyệt qua các node cha
            for (int i = 0; i < keys.length - 1; i++) {
                if (current.has(keys[i]) && current.get(keys[i]).isJsonObject()) {
                    current = current.getAsJsonObject(keys[i]);
                } else {
                    // Nếu node cha không tồn tại, tạo mới
                    JsonObject newNode = new JsonObject();
                    current.add(keys[i], newNode);
                    current = newNode;
                }
            }

            // Cập nhật giá trị ở node cuối cùng
            current.addProperty(keys[keys.length - 1], newValue);
            saveFile(filePath, root);
        } catch (Exception e) {
            LogUtils.error("Lỗi khi update Nested Key: " + compositeKey);
        }
    }

    /**
     * Xóa một key trong file JSON
     */
    public static void removeKey(String filePath, String key) {
        JsonObject jsonObject = getJsonObject(filePath);
        if (jsonObject.has(key)) {
            jsonObject.remove(key);
            saveFile(filePath, jsonObject);
            LogUtils.info("Đã xóa key: " + key);
        }
    }

    /**
     * Hàm hỗ trợ ghi đè dữ liệu vào file
     */
    private static void saveFile(String filePath, JsonObject jsonObject) {
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            LogUtils.error("Lỗi khi lưu file JSON: " + e.getMessage());
        }
    }
}