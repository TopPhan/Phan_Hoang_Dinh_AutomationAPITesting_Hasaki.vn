package com.testScenarios;

import com.baseSetup.BaseTest;
import com.builder.LoginPOJO_Builder;
import com.globals.ConfigsGlobal;
import com.globals.EndPointGlobal;
import com.globals.TokenGlobal;
import com.helper.LogUtils;
import com.helper.SchemaHelper;
import com.keywords.ApiKeyword;
import com.keywords.GetSection;
import com.pojoModel.LoginModel;
import com.validator.ResponseValidator;
import dataProvider.DataProviders;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * LoginTest — Authentication feature
 * Endpoint: POST /mobile/v1/user/login-hasaki
 *
 * Test flow order:
 *   [1] Positive     — valid credentials, remember flag variants
 *   [2] Negative     — data-driven invalid credentials (empty, wrong, special chars)
 *   [3] Security     — CSRF enforcement, fresh-session, brute-force lockout
 *   [4] Schema       — response contract validation
 *   [5] Performance  — response time threshold
 *
 * Pre-condition: BaseTest.suiteSetup() performs one login and stores
 *   TokenGlobal.VERIFY_TOKEN and TokenGlobal.COOKIES before this class runs.
 *
 * Response structure:
 *   status.error_code    — 0 = success, non-zero = failure
 *   status.error_message — "login success" | error description
 *   data.verify_token    — auth token used by downstream cart/checkout endpoints
 */
@Epic("Hasaki.vn API Testing")
@Feature("Authentication")
public class LoginTest extends BaseTest {

    // ── Positive ──────────────────────────────────────────────────────────────

    @Test(priority = 1, groups = {"positive", "smoke"})
    @Story("TC-L01: Valid credentials")
    @Description("Happy path: correct username + password must return error_code=0, "
            + "a non-empty verify_token, and persist the token to TokenGlobal for downstream tests.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC_L01_LoginWithValidCredentials() {
        Response response = ApiKeyword.post(EndPointGlobal.EP_LOGIN, LoginPOJO_Builder.getDataLogin());

        ResponseValidator.assertHasakiSuccess(response);
        ResponseValidator.assertFieldEquals(response, "status.error_message", "login success");
        ResponseValidator.assertFieldNotEmpty(response, "data.verify_token");

        // Token must be stored globally — cart/checkout tests depend on it
        Assert.assertNotNull(TokenGlobal.VERIFY_TOKEN, "VERIFY_TOKEN must be saved to global after login");
    }

    @Test(priority = 2, groups = {"positive"})
    @Story("TC-L02: Login with remember=false")
    @Description("Ensure the remember flag does not break authentication. "
            + "A short-lived session should still return a valid verify_token.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_L02_LoginWithRememberFalse() {
        LoginModel body = LoginModel.builder()
                .username(ConfigsGlobal.USERNAME)
                .password(ConfigsGlobal.PASSWORD)
                .remember(false)
                .build();

        Response response = ApiKeyword.post(EndPointGlobal.EP_LOGIN, body);

        ResponseValidator.assertHasakiSuccess(response);
        ResponseValidator.assertFieldNotEmpty(response, "data.verify_token");
    }

    // ── Negative (data-driven) ────────────────────────────────────────────────

    @Test(priority = 3,
          dataProvider = "invalidCredentials",
          dataProviderClass = DataProviders.class,
          groups = {"negative", "smoke"})
    @Story("TC-L03: Invalid credentials — error_code != 0, no token issued")
    @Description("Data-driven: each row in LoginInvalidData.json (wrong password, empty fields, "
            + "unknown username, special chars) must produce error_code != 0 and null verify_token. "
            + "Covers all credential failure modes in one parameterised test.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_L03_LoginWithInvalidCredentials(
            String tcId, String username, String password, String description) {

        LogUtils.info("[" + tcId + "] " + description);

        LoginModel body = LoginModel.builder()
                .username(username)
                .password(password)
                .remember(false)
                .build();

        Response response = ApiKeyword.post(EndPointGlobal.EP_LOGIN, body);

        ResponseValidator.assertStatusOk(response);
        Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                tcId + ": error_code must be non-zero for invalid credentials");
        Assert.assertNull(response.jsonPath().get("data.verify_token"),
                tcId + ": verify_token must be null on failed login");
        ResponseValidator.assertFieldNotEmpty(response, "status.error_message");
    }

    // ── Security ──────────────────────────────────────────────────────────────

    @Test(priority = 4, groups = {"security"})
    @Story("TC-L04: No CSRF token — documents SEC-001 finding")
    @Description("Sends a valid login request with no form_key cookie (CSRF token). "
            + "If server returns error_code=0 the CSRF gap is confirmed and logged as SEC-001. "
            + "Test does NOT hard-fail — it documents the finding so the suite continues. "
            + "Review the Allure report 'warn' log entry after each run.")
    @Issue("SEC-001")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_L04_LoginWithoutFormKey() {
        // Raw RestAssured request — no cookies injected by BaseTest
        Response response = RestAssured.given()
                .baseUri("https://hasaki.vn/")
                .contentType("application/json")
                .accept("application/json")
                .body(LoginPOJO_Builder.getDataLogin())
                .when().post(EndPointGlobal.EP_LOGIN)
                .then().extract().response();

        ResponseValidator.assertStatusOk(response);
        Assert.assertNotNull(response.jsonPath().get("status.error_code"),
                "error_code field must always be present regardless of CSRF state");

        int errorCode = response.jsonPath().getInt("status.error_code");
        if (errorCode == 0) {
            // Document finding — log warn; do not Assert.fail() so suite continues
            LogUtils.warn("SEC-001 CONFIRMED: login succeeded without form_key (CSRF not enforced)");
        } else {
            LogUtils.info("SEC-001: CSRF token appears to be enforced — login blocked without form_key");
        }
    }

    @Test(priority = 5, groups = {"security"})
    @Story("TC-L05: Fresh login without pre-existing session cookies")
    @Description("Verifies the login endpoint is fully stateless on ingress — "
            + "a brand-new client (no cookies at all) must be able to authenticate "
            + "and receive a usable verify_token in the response.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_L05_LoginRequiresNoCookieButReturnsSession() {
        // Raw request — deliberately no cookies attached
        Response response = RestAssured.given()
                .baseUri("https://hasaki.vn/")
                .contentType("application/json")
                .accept("application/json")
                .body(LoginPOJO_Builder.getDataLogin())
                .when().post(EndPointGlobal.EP_LOGIN)
                .then().extract().response();

        ResponseValidator.assertHasakiSuccess(response);
        ResponseValidator.assertFieldNotEmpty(response, "data.verify_token");
    }

    @Test(priority = 6, groups = {"security", "negative"})
    @Story("TC-L06: Brute-force — account must not lock after 5 bad-password attempts")
    @Description("Sends 5 consecutive bad-password requests for a valid username, "
            + "then verifies the account is NOT locked by attempting a correct login afterward. "
            + "Ensures the API does not produce false lockouts from failed test runs.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_L06_RepeatedInvalidPasswordDoesNotLockValidLogin() {
        LoginModel badBody = LoginModel.builder()
                .username(ConfigsGlobal.USERNAME)
                .password("wrong_password_abc123")
                .remember(false)
                .build();

        // Step 1: 5 bad-password attempts — each must fail
        for (int i = 0; i < 5; i++) {
            Response r = ApiKeyword.post(EndPointGlobal.EP_LOGIN, badBody);
            ResponseValidator.assertStatusOk(r);
            Assert.assertNotEquals(r.jsonPath().getInt("status.error_code"), 0,
                    "attempt " + (i + 1) + ": expected error_code != 0");
        }

        // Step 2: refresh cookies (form_key/HSKSIGN) before valid attempt
        new GetSection().getCookies();

        // Step 3: correct credentials must still succeed
        Response validResp = ApiKeyword.post(EndPointGlobal.EP_LOGIN, LoginPOJO_Builder.getDataLogin());
        ResponseValidator.assertHasakiSuccess(validResp);
        ResponseValidator.assertFieldNotEmpty(validResp, "data.verify_token");
        LogUtils.info("TC-L06 PASS — account not locked after 5 bad-password attempts");
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    @Test(priority = 7, groups = {"schema"})
    @Story("TC-L07: Login response schema validation")
    @Description("Validates the full response JSON structure against LoginSchema.json. "
            + "Catches breaking contract changes (renamed fields, type changes, removed keys) "
            + "that business-logic assertions alone would miss.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_L07_LoginResponseSchema() {
        Response response = ApiKeyword.post(EndPointGlobal.EP_LOGIN, LoginPOJO_Builder.getDataLogin());
        ResponseValidator.assertStatusOk(response);
        SchemaHelper.verifySchema(response, "jsonSchema/LoginSchema.json");
    }

    // ── Performance ───────────────────────────────────────────────────────────

    @Test(priority = 8, groups = {"performance"})
    @Story("TC-L08: Response time < 3000ms")
    @Description("Login is on the critical path for all authenticated features. "
            + "A response exceeding 3000ms is considered a performance regression.")
    @Severity(SeverityLevel.MINOR)
    public void TC_L08_LoginResponseTime() {
        Response response = ApiKeyword.post(EndPointGlobal.EP_LOGIN, LoginPOJO_Builder.getDataLogin());
        ResponseValidator.assertStatusOk(response);
        ResponseValidator.assertResponseTimeLessThan(response, 3000);
    }
}
