package com.baseSetup;

import com.builder.LoginPOJO_Builder;
import com.globals.EndPointGlobal;
import com.globals.TokenGlobal;
import com.helper.LogUtils;
import com.keywords.ApiKeyword;
import com.keywords.GetSection;
import io.restassured.response.Response;
import org.testng.annotations.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Common test setup executed before each test class.
 *
 * Chiến lược cho Suite run (nhiều class chạy liên tiếp):
 *
 *   - getCookies() luôn được gọi để refresh form_key + HSKSIGN (short-lived tokens).
 *     Đây là nguyên nhân 401 khi chạy suite: HSKSIGN expire giữa các class.
 *
 *   - login() chỉ gọi khi chưa có HASAKI_SESSID hợp lệ (session chưa tồn tại).
 *     Tránh trigger cơ chế verify_device của Hasaki khi login quá nhiều lần.
 *
 * Kết quả: class đầu tiên → getCookies + login; các class sau → chỉ getCookies (refresh token).
 */
public class BaseTest {

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        LogUtils.info("=== Suite Setup: initializing session ===");
        new GetSection().getCookies();

        Response loginResponse = ApiKeyword.post(
                EndPointGlobal.EP_LOGIN, LoginPOJO_Builder.getDataLogin());

        int errorCode = loginResponse.jsonPath().getInt("status.error_code");
        if (errorCode == 0) {
            mergeCookies(loginResponse);
            extractPostLoginTokens(loginResponse);
            LogUtils.info("Suite Setup: login OK — HASAKI_SESSID present: "
                    + TokenGlobal.COOKIES.containsKey("HASAKI_SESSID"));
        } else {
            LogUtils.error("Suite Setup: login FAILED — errorCode=" + errorCode);
        }
    }

    @BeforeClass
    public void loginAndGetSession() {
        // Chỉ refresh HSKSIGN + form_key, KHÔNG login lại
        new GetSection().getCookies();

        boolean hasSession = TokenGlobal.COOKIES != null
                && TokenGlobal.COOKIES.containsKey("HASAKI_SESSID")
                && TokenGlobal.VERIFY_TOKEN != null;

        if (!hasSession) {
            // Fallback nếu suite setup thất bại
            LogUtils.warn("BeforeClass: no session — re-login fallback");
            Response r = ApiKeyword.post(EndPointGlobal.EP_LOGIN, LoginPOJO_Builder.getDataLogin());
            if (r.jsonPath().getInt("status.error_code") == 0) {
                mergeCookies(r);
                extractPostLoginTokens(r);
            }
        } else {
            // Merge lại cookies mới từ getCookies() vào session hiện tại
            // để giữ HASAKI_SESSID nhưng có form_key/HSKSIGN mới
            LogUtils.info("BeforeClass: session OK, refreshed form_key + HSKSIGN");
        }
    }

    private void mergeCookies(Response loginResponse) {
        Map<String, String> merged = new HashMap<>();
        if (TokenGlobal.COOKIES != null)
            merged.putAll(TokenGlobal.COOKIES);

        Map<String, String> newCookies = loginResponse.getCookies();
        if (newCookies != null)
            merged.putAll(newCookies);

        TokenGlobal.COOKIES = merged;
    }

    private void extractPostLoginTokens(Response loginResponse) {
        String newFormKey = loginResponse.getCookie("form_key");
        if (newFormKey != null)
            TokenGlobal.FORMKEY = newFormKey;

        String verifyToken = loginResponse.jsonPath().getString("data.verify_token");
        if (verifyToken != null)
            TokenGlobal.VERIFY_TOKEN = verifyToken;

        String hskSign = loginResponse.getCookie("HSKSIGN");
        if (hskSign != null)
            TokenGlobal.HSKSIGN = hskSign;
    }
}
