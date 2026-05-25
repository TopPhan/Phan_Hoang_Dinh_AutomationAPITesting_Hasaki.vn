package com.globals;

import com.helper.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * EnvConfig — load đúng file config theo môi trường chỉ định bằng -Denv.
 *
 * Thứ tự ưu tiên credential (cao → thấp):
 *   1. JVM system property:  -DUSERNAME=xxx  -DPASSWORD=yyy  (CLI / CI)
 *   2. Environment variable: USERNAME / PASSWORD              (OS env / GitHub Secrets)
 *   3. File .properties của env đang chạy                    (local fallback)
 *
 * Cách chạy:
 *   mvn test                          → load dev.properties   (mặc định)
 *   mvn test -Denv=staging            → load staging.properties
 *   mvn test -Denv=prod               → load prod.properties
 *
 * Vị trí file config:
 *   src/test/resources/configs/dev.properties
 *   src/test/resources/configs/staging.properties
 *   src/test/resources/configs/prod.properties
 */
public class EnvConfig {

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static final EnvConfig INSTANCE = new EnvConfig();
    private final Properties props = new Properties();
    private final String activeEnv;

    private EnvConfig() {
        // Đọc -Denv từ command line, mặc định là "dev"
        activeEnv = System.getProperty("env", "dev").toLowerCase().trim();
        String resourcePath = "configs/" + activeEnv + ".properties";

        try (InputStream is = EnvConfig.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException(
                        "[EnvConfig] Config file not found on classpath: " + resourcePath
                        + "\nExpected location: src/test/resources/" + resourcePath
                        + "\nAvailable envs: dev | staging | prod");
            }
            props.load(is);
            LogUtils.info("[EnvConfig] Loaded environment: " + activeEnv.toUpperCase()
                    + " | Base URI: " + props.getProperty("BASE_URI", "(not set)"));
        } catch (IOException e) {
            throw new RuntimeException("[EnvConfig] Failed to load config: " + resourcePath, e);
        }
    }

    public static EnvConfig getInstance() {
        return INSTANCE;
    }

    // ── Property getters ───────────────────────────────────────────────────────

    /**
     * Lấy giá trị từ file .properties của env hiện tại.
     * Trả về null nếu key không tồn tại.
     */
    public String get(String key) {
        return props.getProperty(key);
    }

    /**
     * Lấy giá trị với fallback nếu key không tồn tại.
     */
    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * Lấy giá trị int.
     */
    public int getInt(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            LogUtils.warn("[EnvConfig] Key [" + key + "] is not a valid int: " + val);
            return defaultValue;
        }
    }

    /**
     * Lấy giá trị boolean.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.trim());
    }

    /** Tên môi trường đang chạy (dev / staging / prod). */
    public String getActiveEnv() {
        return activeEnv;
    }

    // ── Credential resolution (system prop → env var → file) ──────────────────

    /**
     * Resolve USERNAME theo thứ tự ưu tiên:
     *   1. -DUSERNAME=xxx (maven command line)
     *   2. env var USERNAME
     *   3. configs/{env}.properties
     */
    public String getUsername() {
        return resolveCredential("CHROME_USER", "USERNAME");
    }

    /**
     * Resolve PASSWORD theo thứ tự ưu tiên.
     */
    public String getPassword() {
        return resolveCredential("CHROME_PASS", "PASSWORD");
    }

    private String resolveCredential(String sysPropKey, String fileKey) {
        // 1. JVM system property (-DUSERNAME=xxx)
        String sysProp = System.getProperty(sysPropKey);
        if (sysProp != null && !sysProp.isBlank()) return sysProp;

        // 2. OS environment variable
        String envVar = System.getenv(sysPropKey);
        if (envVar != null && !envVar.isBlank()) return envVar;

        // 3. File .properties
        String fileProp = props.getProperty(fileKey, "");
        if (!fileProp.isBlank()) return fileProp;

        LogUtils.warn("[EnvConfig] Credential [" + fileKey + "] not found in any source "
                + "(system property, env var, or " + activeEnv + ".properties)");
        return "";
    }
}
