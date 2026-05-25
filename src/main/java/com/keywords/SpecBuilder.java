package com.keywords;

import com.globals.ConfigsGlobal;
import com.globals.TokenGlobal;
import com.google.gson.Gson;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.mapper.factory.GsonObjectMapperFactory;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

/**
 * Centralised REST-Assured spec factory.
 *
 * Three spec variants:
 *   - getRequestSpecBuilder()        authenticated JSON requests (cookies + form_key + verify_token)
 *   - getHasakiFormSpecBuilder()     form-urlencoded POST (used where server requires it)
 *   - getRequestNotAuthSpecBuilder() unauthenticated JSON requests (for negative/security tests)
 */
public class SpecBuilder {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";

    public static RequestSpecification getRequestSpecBuilder() {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(ConfigsGlobal.BASE_URI)
                .setBasePath(ConfigsGlobal.BASE_PATH)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("User-Agent",        USER_AGENT)
                .addHeader("Accept-Language",   "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                .addHeader("sec-ch-ua",         "\"Chromium\";v=\"136\", \"Google Chrome\";v=\"136\", \"Not.A/Brand\";v=\"99\"")
                .addHeader("sec-ch-ua-mobile",  "?0")
                .addHeader("sec-ch-ua-platform","\"Windows\"")
                .addHeader("Sec-Fetch-Dest",    "empty")
                .addHeader("Sec-Fetch-Mode",    "cors")
                .addHeader("Sec-Fetch-Site",    "same-origin")
                .addHeader("Origin",            "https://hasaki.vn")
                .addHeader("Referer",           "https://hasaki.vn/")
                .addFilter(new AllureRestAssured());

        if (TokenGlobal.FORMKEY != null)
            builder.addQueryParam("form_key", TokenGlobal.FORMKEY);

        if (TokenGlobal.COOKIES != null && !TokenGlobal.COOKIES.isEmpty())
            builder.addCookies(TokenGlobal.COOKIES);

        if (TokenGlobal.VERIFY_TOKEN != null && !TokenGlobal.VERIFY_TOKEN.isEmpty())
            builder.addHeader("token", TokenGlobal.VERIFY_TOKEN);

        if (TokenGlobal.HSKSIGN != null && !TokenGlobal.HSKSIGN.isEmpty()) {
            builder.addHeader("HSKSIGN", TokenGlobal.HSKSIGN);
            builder.addCookie("HSKSIGN",  TokenGlobal.HSKSIGN);
        }

        if (TokenGlobal.MOBILE_DEVICE_ID != null)
            builder.addHeader("mobiledeviceid", TokenGlobal.MOBILE_DEVICE_ID);

        return builder.build();
    }

    public static RequestSpecification getHasakiFormSpecBuilder() {
        return new RequestSpecBuilder()
                .setBaseUri(ConfigsGlobal.BASE_URI)
                .setBasePath(ConfigsGlobal.BASE_PATH)
                .addQueryParam("form_key", TokenGlobal.FORMKEY)
                .addCookies(TokenGlobal.COOKIES)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .build();
    }

    public static RequestSpecification getRequestNotAuthSpecBuilder() {
        return new RequestSpecBuilder()
                .setBaseUri(ConfigsGlobal.BASE_URI)
                .setBasePath(ConfigsGlobal.BASE_PATH)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .build();
    }

    /**
     * Response spec validates Content-Type only.
     * Logging is handled by ApiKeyword — do NOT add .log() here to avoid double output.
     */
    public static ResponseSpecification getResponseSpecBuilder() {
        return new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .build();
    }
}
