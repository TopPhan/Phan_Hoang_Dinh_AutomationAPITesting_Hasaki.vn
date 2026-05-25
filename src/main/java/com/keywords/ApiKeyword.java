package com.keywords;

import com.globals.ConfigsGlobal;
import com.globals.TokenGlobal;
import com.helper.LogUtils;
import com.reports.AllureManager;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;

import java.io.File;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Thin wrapper around REST-Assured for common HTTP verbs.
 * Each method logs the request path and the full response body exactly once.
 */
public class ApiKeyword {

    @Step("GET {0}")
    public static Response get(String path) {
        LogUtils.info("GET: " + path);
        Response response = given(SpecBuilder.getRequestSpecBuilder())
                .when().get(path)
                .then().spec(SpecBuilder.getResponseSpecBuilder())
                .extract().response();
        logResponse(response);
        return response;
    }

    @Step("GET {0} | headers: {1}")
    public static Response get(String path, Map<String, String> headers) {
        LogUtils.info("GET: " + path + " | headers: " + headers);
        Response response = given(SpecBuilder.getRequestSpecBuilder().headers(headers))
                .when().get(path)
                .then().spec(SpecBuilder.getResponseSpecBuilder())
                .extract().response();
        logResponse(response);
        return response;
    }

    @Step("GET {0} | Bearer token")
    public static Response get(String path, String bearerToken) {
        LogUtils.info("GET: " + path + " | with bearer token");
        Response response = given(SpecBuilder.getRequestSpecBuilder()
                .header("Authorization", "Bearer " + bearerToken))
                .when().get(path)
                .then().spec(SpecBuilder.getResponseSpecBuilder())
                .extract().response();
        logResponse(response);
        return response;
    }

    @Step("GET (no auth) {0}")
    public static Response getNotAuth(String path) {
        LogUtils.info("GET (unauthenticated): " + path);
        Response response = given(SpecBuilder.getRequestNotAuthSpecBuilder())
                .when().get(path)
                .then().spec(SpecBuilder.getResponseSpecBuilder())
                .extract().response();
        logResponse(response);
        return response;
    }

    @Step("POST {0}")
    public static Response post(String path, Object payload) {
        LogUtils.info("POST: " + path + " | body: " + payload);
        Response response = given(SpecBuilder.getRequestSpecBuilder())
                .body(payload)
                .when().post(path)
                .then().spec(SpecBuilder.getResponseSpecBuilder())
                .extract().response();
        logResponse(response);
        return response;
    }

    @Step("POST form {0}")
    public static Response postForm(String path, Map<String, String> formParams) {
        LogUtils.info("POST form: " + path + " | params: " + formParams);
        Response response = given()
                .baseUri(ConfigsGlobal.BASE_URI)
                .basePath(ConfigsGlobal.BASE_PATH)
                .cookies(TokenGlobal.COOKIES)
                .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                .accept(ContentType.JSON)
                .formParams(formParams)
                .queryParam("form_key", TokenGlobal.FORMKEY)
                .filter(new AllureRestAssured())
                .log().all()
                .when().post(path)
                .then().spec(SpecBuilder.getResponseSpecBuilder())
                .extract().response();
        logResponse(response);
        return response;
    }

    @Step("POST (no auth) {0}")
    public static Response postNotAuth(String path, Object payload) {
        LogUtils.info("POST (unauthenticated): " + path + " | body: " + payload);
        Response response = given(SpecBuilder.getRequestNotAuthSpecBuilder())
                .body(payload)
                .when().post(path)
                .then().spec(SpecBuilder.getResponseSpecBuilder())
                .extract().response();
        logResponse(response);
        return response;
    }

    @Step("POST {0} | file body")
    public static Response post(String path, File fileBody) {
        LogUtils.info("POST: " + path + " | file: " + fileBody.getPath());
        Response response = given(SpecBuilder.getRequestSpecBuilder())
                .body(fileBody)
                .when().post(path)
                .then().spec(SpecBuilder.getResponseSpecBuilder())
                .extract().response();
        logResponse(response);
        return response;
    }

    @Step("PUT {0}")
    public static Response put(String path, Object payload) {
        LogUtils.info("PUT: " + path + " | body: " + payload);
        Response response = given(SpecBuilder.getRequestSpecBuilder())
                .body(payload)
                .when().put(path)
                .then().extract().response();
        logResponse(response);
        return response;
    }

    @Step("DELETE {0}")
    public static Response delete(String path, Object payload) {
        LogUtils.info("DELETE: " + path + " | body: " + payload);
        Response response = given(SpecBuilder.getRequestSpecBuilder())
                .body(payload)
                .when().delete(path)
                .then().extract().response();
        logResponse(response);
        return response;
    }

    // ── Response extraction helpers ──────────────────────────────

    @Step("Get response field: {1}")
    public static String getResponseKeyValue(Response response, String key) {
        String value = response.jsonPath().get(key).toString();
        LogUtils.info("Field [" + key + "]: " + value);
        AllureManager.saveTextLog("Field [" + key + "]: " + value);
        return value;
    }

    @Step("Get response field: {1}")
    public static String getResponseKeyValue(String responseBody, String key) {
        String value = new JsonPath(responseBody).get(key).toString();
        LogUtils.info("Field [" + key + "]: " + value);
        AllureManager.saveTextLog("Field [" + key + "]: " + value);
        return value;
    }

    // ── Assertion helpers ────────────────────────────────────────

    @Step("Assert status code == {1}")
    public static void verifyStatusCode(Response response, int expected) {
        int actual = response.getStatusCode();
        LogUtils.info("Status code: " + actual + " == " + expected);
        AllureManager.saveTextLog("Status code: " + actual + " == " + expected);
        Assert.assertEquals(actual, expected, "Status code mismatch.");
    }

    @Step("Get status code")
    public static int getStatusCode(Response response) {
        int code = response.getStatusCode();
        LogUtils.info("Status code: " + code);
        AllureManager.saveTextLog("Status code: " + code);
        return code;
    }

    @Step("Get status line")
    public static String getStatusLine(Response response) {
        String line = response.getStatusLine();
        LogUtils.info("Status line: " + line);
        AllureManager.saveTextLog("Status line: " + line);
        return line;
    }

    @Step("Get header: {1}")
    public static String getResponseHeader(Response response, String headerKey) {
        String value = response.getHeader(headerKey);
        LogUtils.info("Header [" + headerKey + "]: " + value);
        AllureManager.saveTextLog("Header [" + headerKey + "]: " + value);
        return value;
    }

    @Step("Get Content-Type")
    public static String getResponseContentType(Response response) {
        String contentType = response.getContentType();
        LogUtils.info("Content-Type: " + contentType);
        AllureManager.saveTextLog("Content-Type: " + contentType);
        return contentType;
    }

    @Step("Get cookie: {1}")
    public static String getResponseCookieName(Response response, String cookieName) {
        String value = response.getCookie(cookieName);
        LogUtils.info("Cookie [" + cookieName + "]: " + value);
        AllureManager.saveTextLog("Cookie [" + cookieName + "]: " + value);
        return value;
    }

    // ── Private ──────────────────────────────────────────────────

    private static void logResponse(Response response) {
        LogUtils.info("RESPONSE:\n" + response.asPrettyString());
        AllureManager.saveTextLog("RESPONSE:\n" + response.asPrettyString());
    }
}
