package com.keywords;

import com.globals.ConfigsGlobal;
import com.globals.TokenGlobal;
import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Bootstraps a browser-like session before any authenticated API call.
 *
 * Fetches the Hasaki homepage to receive a valid form_key (CSRF token) and
 * session cookies, then merges them with the long-lived device/tracking cookies
 * configured in configs.properties.
 *
 * Call order in BaseTest:
 *   1. getCookies()  — establish session + CSRF
 *   2. login()       — authenticate, capture verify_token + HSKSIGN
 */
public class GetSection {

    private static final String BASE_URL    = "https://hasaki.vn/mobile/v1";
    private static final String USER_AGENT  =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36";

    public void getCookies() {
        Map<String, String> deviceCookies = buildDeviceCookies();

        Response response = RestAssured.given()
                .filter(new SessionFilter())
                .cookies(deviceCookies)
                .header("User-Agent",        USER_AGENT)
                .header("Accept",            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language",   "en-US,en;q=0.9,vi-VN;q=0.8,vi;q=0.7")
                .header("mobiledeviceid",    ConfigsGlobal.MOBILE_DEVICE_ID)
                .header("sec-ch-ua",         "\"Chromium\";v=\"148\", \"Google Chrome\";v=\"148\", \"Not/A)Brand\";v=\"99\"")
                .header("sec-ch-ua-mobile",  "?0")
                .header("sec-ch-ua-platform","\"Windows\"")
                .header("Sec-Fetch-Dest",    "document")
                .header("Sec-Fetch-Mode",    "navigate")
                .header("Sec-Fetch-Site",    "none")
                .get(BASE_URL);

        // Merge: server-returned cookies take precedence over device defaults
        Map<String, String> merged = new HashMap<>(deviceCookies);
        merged.putAll(response.getCookies());

        TokenGlobal.FORMKEY          = merged.getOrDefault("form_key", response.getCookie("form_key"));
        TokenGlobal.COOKIES          = merged;
        TokenGlobal.MOBILE_DEVICE_ID = ConfigsGlobal.MOBILE_DEVICE_ID;
    }

    private Map<String, String> buildDeviceCookies() {
        Map<String, String> cookies = new HashMap<>();
        cookies.put("mobiledeviceid", ConfigsGlobal.MOBILE_DEVICE_ID);
        cookies.put("UUID",           ConfigsGlobal.BROWSER_UUID);
        cookies.put("hsk_cdp_uid",    ConfigsGlobal.HSK_CDP_UID);
        cookies.put("hsk_cdp_cid",    ConfigsGlobal.HSK_CDP_CID);
        cookies.put("cookieConsent",  "1");
        cookies.put("__RC",           "5");
        return cookies;
    }
}
