package com.globals;

import java.util.Map;

/**
 * Shared session state populated by GetSection and BaseTest.
 * Fields are intentionally mutable to allow sequential setup (getCookies → login).
 */
public class TokenGlobal {

    public static String              TOKEN;
    public static String              FORMKEY;
    public static String              VERIFY_TOKEN;
    public static String              HSKSIGN;
    public static String              MOBILE_DEVICE_ID;
    public static Map<String, String> COOKIES;

    public static String              getBearerToken()  { return TOKEN; }
    public static String              getFormKey()      { return FORMKEY; }
    public static String              getVerifyToken()  { return VERIFY_TOKEN; }
    public static Map<String, String> getCookies()      { return COOKIES; }
}
