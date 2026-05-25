package com.validator;

import com.helper.LogUtils;
import com.reports.AllureManager;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.List;

/**
 * Centralizes all assertion logic for the Hasaki API.
 *
 * When the response structure changes (e.g. "status.error_code" → "meta.code"),
 * update this class once instead of touching every test file.
 */
public class ResponseValidator {

    // ── HTTP Status ──────────────────────────────────────────────────────────

    @Step("Assert HTTP 200 OK")
    public static void assertStatusOk(Response response) {
        assertStatusCode(response, 200);
    }

    @Step("Assert HTTP status code == {1}")
    public static void assertStatusCode(Response response, int expected) {
        int actual = response.getStatusCode();
        String msg = "HTTP Status: expected=" + expected + ", actual=" + actual;
        LogUtils.info(msg);
        AllureManager.saveTextLog(msg);
        Assert.assertEquals(actual, expected,
                "HTTP status code mismatch — expected " + expected + " but got " + actual);
    }

    // ── Hasaki Business Error Code ───────────────────────────────────────────

    @Step("Assert Hasaki error_code == 0 (success)")
    public static void assertErrorCodeSuccess(Response response) {
        assertErrorCode(response, 0);
    }

    /** Verify a specific error_code — useful for negative tests (e.g. 401, 422). */
    @Step("Assert Hasaki error_code == {1}")
    public static void assertErrorCode(Response response, int expectedCode) {
        int actual = response.jsonPath().getInt("status.error_code");
        String msg = "Hasaki error_code: expected=" + expectedCode + ", actual=" + actual;
        LogUtils.info(msg);
        AllureManager.saveTextLog(msg);
        Assert.assertEquals(actual, expectedCode,
                "error_code mismatch — expected " + expectedCode + " but got " + actual);
    }

    /** Verify error_code is one of the accepted values. */
    @Step("Assert Hasaki error_code is one of {1}")
    public static void assertErrorCodeIn(Response response, int... acceptedCodes) {
        int actual = response.jsonPath().getInt("status.error_code");
        for (int code : acceptedCodes) {
            if (actual == code) {
                LogUtils.info("error_code=" + actual + " is accepted.");
                AllureManager.saveTextLog("error_code=" + actual + " is accepted.");
                return;
            }
        }
        String msg = "error_code=" + actual + " not in accepted list";
        LogUtils.error(msg);
        Assert.fail(msg);
    }

    // ── Combined Assertions ──────────────────────────────────────────────────

    /** HTTP 200 + error_code=0. Use for happy-path tests. */
    @Step("Assert Hasaki API success (HTTP 200 + error_code 0)")
    public static void assertHasakiSuccess(Response response) {
        assertStatusOk(response);
        assertErrorCodeSuccess(response);
    }

    /** HTTP 200 + specific error_code. Use for negative tests where server still returns 200. */
    @Step("Assert Hasaki API error (HTTP 200 + error_code {1})")
    public static void assertHasakiError(Response response, int expectedErrorCode) {
        assertStatusOk(response);
        assertErrorCode(response, expectedErrorCode);
    }

    // ── Field Assertions ─────────────────────────────────────────────────────

    @Step("Assert field [{1}] == \"{2}\"")
    public static void assertFieldEquals(Response response, String jsonPath, String expected) {
        String actual = response.jsonPath().getString(jsonPath);
        String msg = "Field [" + jsonPath + "]: expected=\"" + expected + "\", actual=\"" + actual + "\"";
        LogUtils.info(msg);
        AllureManager.saveTextLog(msg);
        Assert.assertEquals(actual, expected, "Field mismatch at [" + jsonPath + "]");
    }

    @Step("Assert field [{1}] == {2}")
    public static void assertFieldEquals(Response response, String jsonPath, int expected) {
        int actual = response.jsonPath().getInt(jsonPath);
        String msg = "Field [" + jsonPath + "]: expected=" + expected + ", actual=" + actual;
        LogUtils.info(msg);
        AllureManager.saveTextLog(msg);
        Assert.assertEquals(actual, expected, "Field mismatch at [" + jsonPath + "]");
    }

    @Step("Assert field [{1}] == {2}")
    public static void assertFieldEquals(Response response, String jsonPath, boolean expected) {
        boolean actual = response.jsonPath().getBoolean(jsonPath);
        String msg = "Field [" + jsonPath + "]: expected=" + expected + ", actual=" + actual;
        LogUtils.info(msg);
        AllureManager.saveTextLog(msg);
        Assert.assertEquals(actual, expected, "Field mismatch at [" + jsonPath + "]");
    }

    /** Verify a field is present and non-empty (e.g. tokens, session IDs). */
    @Step("Assert field [{1}] is not null/empty")
    public static void assertFieldNotEmpty(Response response, String jsonPath) {
        String actual = response.jsonPath().getString(jsonPath);
        String msg = "Field [" + jsonPath + "]: \"" + actual + "\"";
        LogUtils.info(msg);
        AllureManager.saveTextLog(msg);
        Assert.assertNotNull(actual, "Field [" + jsonPath + "] is null");
        Assert.assertFalse(actual.trim().isEmpty(), "Field [" + jsonPath + "] is empty");
    }

    // ── List / Array Assertions ──────────────────────────────────────────────

    /** Verify a list field is present and non-empty. */
    @Step("Assert list [{1}] is not empty")
    public static void assertListNotEmpty(Response response, String jsonPath) {
        List<?> list = response.jsonPath().getList(jsonPath);
        String msg = "List [" + jsonPath + "]: size=" + (list != null ? list.size() : "null");
        LogUtils.info(msg);
        AllureManager.saveTextLog(msg);
        Assert.assertNotNull(list, "List [" + jsonPath + "] is null");
        Assert.assertFalse(list.isEmpty(), "List [" + jsonPath + "] is empty");
    }

    @Step("Assert list [{1}] has at least {2} item(s)")
    public static void assertListMinSize(Response response, String jsonPath, int minSize) {
        List<?> list = response.jsonPath().getList(jsonPath);
        int actual = list != null ? list.size() : 0;
        String msg = "List [" + jsonPath + "]: size=" + actual + " (min=" + minSize + ")";
        LogUtils.info(msg);
        AllureManager.saveTextLog(msg);
        Assert.assertTrue(actual >= minSize,
                "List [" + jsonPath + "] has " + actual + " items, expected >= " + minSize);
    }

    // ── Response Time ────────────────────────────────────────────────────────

    @Step("Assert response time < {1}ms")
    public static void assertResponseTimeLessThan(Response response, long maxMillis) {
        long actual = response.getTime();
        String msg = "Response time: " + actual + "ms (max=" + maxMillis + "ms)";
        LogUtils.info(msg);
        AllureManager.saveTextLog(msg);
        Assert.assertTrue(actual < maxMillis,
                "Response too slow: " + actual + "ms > " + maxMillis + "ms");
    }

    // ── Header / Cookie Assertions ───────────────────────────────────────────

    @Step("Assert response header [{1}] exists")
    public static void assertHeaderExists(Response response, String headerName) {
        String value = response.getHeader(headerName);
        String msg = "Header [" + headerName + "]: " + value;
        LogUtils.info(msg);
        AllureManager.saveTextLog(msg);
        Assert.assertNotNull(value, "Header [" + headerName + "] not found in response");
    }

    @Step("Assert response cookie [{1}] exists")
    public static void assertCookieExists(Response response, String cookieName) {
        String value = response.getCookie(cookieName);
        String msg = "Cookie [" + cookieName + "]: " + (value != null ? "present" : "MISSING");
        LogUtils.info(msg);
        AllureManager.saveTextLog(msg);
        Assert.assertNotNull(value, "Cookie [" + cookieName + "] not found in response");
        Assert.assertFalse(value.isEmpty(), "Cookie [" + cookieName + "] is empty");
    }
}
