package com.globals;

/**
 * ConfigsGlobal — central config holder, đọc từ EnvConfig.
 *
 * THAY ĐỔI so với phiên bản cũ:
 *   Trước: đọc thẳng từ PropertiesHelper → chỉ load 1 file cố định configs.properties
 *   Sau:   đọc từ EnvConfig → load đúng file theo -Denv (dev / staging / prod)
 *
 * Không cần sửa bất kỳ chỗ nào dùng ConfigsGlobal.BASE_URI, ConfigsGlobal.USERNAME v.v.
 * Chỉ cần thêm -Denv=staging khi chạy mvn là tự động đổi môi trường.
 */
public class ConfigsGlobal {

    private static final EnvConfig ENV = EnvConfig.getInstance();

    // ── Connection ─────────────────────────────────────────────────────────────
    public static final String  BASE_URI  = ENV.get("BASE_URI", "https://hasaki.vn/");
    public static final String  BASE_PATH = ENV.get("BASE_PATH", "");

    // ── Credentials (resolved: system prop → env var → file) ──────────────────
    public static final String  USERNAME  = ENV.getUsername();
    public static final String  PASSWORD  = ENV.getPassword();
    public static final boolean REMEMBER  = ENV.getBoolean("REMEMBER", true);

    // ── Device fingerprinting ──────────────────────────────────────────────────
    public static final String MOBILE_DEVICE_ID = ENV.get("MOBILE.DEVICE.ID", "");
    public static final String BROWSER_UUID     = ENV.get("BROWSER.UUID", "");
    public static final String HSK_CDP_UID      = ENV.get("HSK.CDP.UID", "");
    public static final String HSK_CDP_CID      = ENV.get("HSK.CDP.CID", "");

    // ── Timeout ────────────────────────────────────────────────────────────────
    public static final int CONNECTION_TIMEOUT  = ENV.getInt("CONNECTION.TIMEOUT", 10000);
    public static final int READ_TIMEOUT        = ENV.getInt("READ.TIMEOUT", 15000);

    // ── Runtime counters (updated by TestListener) ─────────────────────────────
    public static int TCS_TOTAL    = 0;
    public static int PASSED_TOTAL = 0;
    public static int FAILED_TOTAL = 0;

    // ── Active env info (for Allure report) ───────────────────────────────────
    public static final String ACTIVE_ENV = ENV.getActiveEnv();
}
