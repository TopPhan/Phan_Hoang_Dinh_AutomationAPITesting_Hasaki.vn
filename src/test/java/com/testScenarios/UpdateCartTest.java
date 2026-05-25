package com.testScenarios;

import com.baseSetup.BaseTest;
import com.globals.EndPointGlobal;
import com.globals.TokenGlobal;
import com.helper.LogUtils;
import com.keywords.ApiKeyword;
import com.pojoModel.AddToCartModel;
import com.pojoModel.UpdateCartModel;
import com.validator.ResponseValidator;
import dataProvider.DataProviders;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * UpdateCartTest — Update Product Quantity in Cart
 * Endpoint: POST /mobile/v2/checkout/cart/update-product
 *
 * Test flow order:
 *   [1] Positive     — increase qty, decrease qty, alert message, sequential updates, large qty
 *   [2] Negative     — negative product_id, non-existent product, product not in cart,
 *                      qty=0, negative qty, partial/empty body
 *   [3] Security     — unauthenticated request, invalid CSRF token
 *   [4] Integration  — E2E flow: Search → Add → Update qty
 *   [5] Performance  — response time threshold
 *   [6] Data-driven  — bulk valid and invalid product/quantity combinations
 *
 * Pre-condition: target product must already exist in the cart before update calls.
 *   ensureInCart() is called at the start of each positive test to guarantee this.
 *
 * Request body:
 *   product_id         — ID of the product already in cart
 *   quantity_selected  — new quantity to set (replaces current quantity)
 *
 * Response structure (success):
 *   status.error_code    — 0 = success
 *   status.alert_message — user-facing confirmation (must be non-empty on success)
 *   data                 — cart summary block (non-null on success)
 */
@Epic("Hasaki.vn API Testing")
@Feature("Update Product In Cart")
public class UpdateCartTest extends BaseTest {

    private static final int PRODUCT_VALID    = 99791;
    private static final int PRODUCT_NONEXIST = 9999999;
    private static final int PRODUCT_INVALID  = -999;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds an update-cart request body.
     *
     * @param productId product ID already in cart
     * @param qty       new quantity to set
     * @return populated UpdateCartModel
     */
    private UpdateCartModel buildBody(int productId, int qty) {
        return UpdateCartModel.builder()
                .productId(productId)
                .quantitySelected(qty)
                .build();
    }

    /**
     * Serialises the body to JSON, logs it, and posts to the update-cart endpoint.
     *
     * @param body request body object
     * @return API response
     */
    private Response callUpdate(Object body) {
        String json = new com.google.gson.Gson().toJson(body);
        LogUtils.info("UpdateCart body: " + json);
        return ApiKeyword.post(EndPointGlobal.EP_UPDATE_CART, json);
    }

    /**
     * Pre-condition helper: adds the given product to the cart so update tests
     * have a valid item to operate on. Logs a warning (does not fail) if the
     * add call fails, to avoid masking the actual update assertion.
     *
     * @param productId product to add
     * @param qty       initial quantity to add
     */
    private void ensureInCart(int productId, int qty) {
        AddToCartModel body = AddToCartModel.builder()
                .product(AddToCartModel.ProductItem.builder()
                        .id(productId).quantitySelected(qty).giftGroupId("").build())
                .build();
        Response r = ApiKeyword.post(EndPointGlobal.EP_ADD_TO_CART,
                new com.google.gson.Gson().toJson(body));
        if (r.jsonPath().getInt("status.error_code") != 0) {
            LogUtils.warn("Pre-condition WARN: add product=" + productId + " failed");
        }
    }

    // ── Positive ──────────────────────────────────────────────────────────────

    @Test(priority = 1, groups = {"positive", "smoke"})
    @Story("TC-U01: Update to qty=2 — error_code=0, data block present")
    @Description("Core smoke test: increasing quantity from 1 to 2 must return error_code=0 "
            + "and a non-null data block. Verifies the update endpoint is reachable and functional.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC_U01_UpdateToQty2() {
        ensureInCart(PRODUCT_VALID, 1);
        Response response = callUpdate(buildBody(PRODUCT_VALID, 2));

        ResponseValidator.assertHasakiSuccess(response);
        Assert.assertNotNull(response.jsonPath().get("data"), "data block must be present on success");
    }

    @Test(priority = 2, groups = {"positive"})
    @Story("TC-U02: Decrease qty 2 → 1 — still succeeds")
    @Description("Verifies that decreasing quantity (2 → 1) is accepted. "
            + "Some APIs reject decreases — this test guards against that regression.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_U02_DecreaseQuantity() {
        ensureInCart(PRODUCT_VALID, 2);
        Response response = callUpdate(buildBody(PRODUCT_VALID, 1));

        ResponseValidator.assertHasakiSuccess(response);
        Assert.assertNotNull(response.jsonPath().get("data"), "data block must be present on success");
    }

    @Test(priority = 3, groups = {"positive"})
    @Story("TC-U03: Success alert_message is non-blank")
    @Description("Verifies the user-facing confirmation message is present after a successful update. "
            + "A blank alert_message would leave the user without visual feedback that the cart changed.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_U03_SuccessAlertMessage() {
        ensureInCart(PRODUCT_VALID, 1);
        Response response = callUpdate(buildBody(PRODUCT_VALID, 2));

        ResponseValidator.assertHasakiSuccess(response);
        ResponseValidator.assertFieldNotEmpty(response, "status.alert_message");
    }

    @Test(priority = 4, groups = {"positive"})
    @Story("TC-U04: Sequential updates 1→2→3→5 all succeed")
    @Description("Performs four consecutive updates on the same product (qty: 1→2→3→5). "
            + "Each update must independently succeed with error_code=0. "
            + "Validates there is no session-level state that blocks repeated updates.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_U04_SequentialUpdates() {
        ensureInCart(PRODUCT_VALID, 1);
        for (int qty : new int[]{1, 2, 3, 5}) {
            Response response = callUpdate(buildBody(PRODUCT_VALID, qty));
            ResponseValidator.assertHasakiSuccess(response);
            LogUtils.info("TC-U04: qty=" + qty + " → PASS");
        }
    }

    @Test(priority = 5, groups = {"positive"})
    @Story("TC-U05: Large qty=999 — must not 5xx, if 200 then response is meaningful")
    @Description("Tests the upper boundary of quantity. The server may accept qty=999 "
            + "(stock check permitting) or reject it with error_code != 0, "
            + "but must never crash with a 5xx. error_code must always be present.")
    @Severity(SeverityLevel.MINOR)
    public void TC_U05_LargeQuantity() {
        ensureInCart(PRODUCT_VALID, 1);
        Response response = callUpdate(buildBody(PRODUCT_VALID, 999));

        Assert.assertTrue(response.getStatusCode() < 500,
                "qty=999 must not return 5xx, got: " + response.getStatusCode());
        Assert.assertNotNull(response.jsonPath().get("status.error_code"),
                "error_code must always be present");
    }

    // ── Negative ──────────────────────────────────────────────────────────────

    @Test(priority = 6, groups = {"negative", "smoke"})
    @Story("TC-U06: Negative product_id — error_code != 0")
    @Description("product_id=-999 is an impossible value and must be rejected. "
            + "Server must return error_code != 0 with a non-empty error_message — "
            + "never a 5xx from unvalidated DB lookup.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_U06_NegativeProductId() {
        Response response = callUpdate(buildBody(PRODUCT_INVALID, 2));

        Assert.assertTrue(response.getStatusCode() < 500,
                "Negative product_id must not return 5xx, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero for product_id=" + PRODUCT_INVALID);
            ResponseValidator.assertFieldNotEmpty(response, "status.error_message");
        }
    }

    @Test(priority = 7, groups = {"negative"})
    @Story("TC-U07: Non-existent product_id — error_code != 0")
    @Description("product_id=9999999 is a positive integer that does not exist. "
            + "Server must look it up, fail gracefully, and return error_code != 0.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_U07_NonExistentProductId() {
        Response response = callUpdate(buildBody(PRODUCT_NONEXIST, 2));

        Assert.assertTrue(response.getStatusCode() < 500,
                "Non-existent product_id must not return 5xx, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero for non-existent product_id=" + PRODUCT_NONEXIST);
        }
    }

    @Test(priority = 8, groups = {"negative"})
    @Story("TC-U08: Product not in cart — error_code != 0")
    @Description("Attempts to update quantity for a product that is valid but almost certainly "
            + "not present in the test user's cart. Server must detect the missing cart line "
            + "and return error_code != 0 rather than silently succeeding.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_U08_ProductNotInCart() {
        Response response = callUpdate(buildBody(12345, 2));

        Assert.assertTrue(response.getStatusCode() < 500,
                "Product-not-in-cart must not return 5xx, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero when product is not in cart");
        }
    }

    @Test(priority = 9, groups = {"negative"})
    @Story("TC-U09: qty=0 — server may treat as remove, must not 5xx")
    @Description("qty=0 is a boundary value: some implementations treat it as a delete. "
            + "Either interpretation is acceptable (error_code 0 or non-zero), "
            + "but a 5xx is not. error_code must always be present in the response.")
    @Severity(SeverityLevel.MINOR)
    public void TC_U09_ZeroQuantity() {
        ensureInCart(PRODUCT_VALID, 2);
        Response response = callUpdate(buildBody(PRODUCT_VALID, 0));

        Assert.assertTrue(response.getStatusCode() < 500,
                "qty=0 must not return 5xx, got: " + response.getStatusCode());
        Assert.assertNotNull(response.jsonPath().get("status.error_code"),
                "error_code must always be present");
    }

    @Test(priority = 10, groups = {"negative"})
    @Story("TC-U10: Negative quantity — error_code != 0")
    @Description("quantity_selected=-1 is semantically invalid for a cart quantity. "
            + "Server must reject it with error_code != 0 and must not apply a negative "
            + "quantity that would corrupt cart totals.")
    @Severity(SeverityLevel.MINOR)
    public void TC_U10_NegativeQuantity() {
        ensureInCart(PRODUCT_VALID, 2);
        Response response = callUpdate(buildBody(PRODUCT_VALID, -1));

        Assert.assertTrue(response.getStatusCode() < 500,
                "Negative qty must not return 5xx, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero for qty=-1");
        }
    }

    @Test(priority = 11, groups = {"negative"})
    @Story("TC-U11: Empty/partial body — must not 5xx")
    @Description("Tests three missing-field scenarios in a single test to avoid duplication:\n"
            + "  (a) empty body '{}'\n"
            + "  (b) missing product_id (only quantity_selected provided)\n"
            + "  (c) missing quantity_selected (only product_id provided)\n"
            + "All three must return HTTP < 500. Required-field validation must be present.")
    @Severity(SeverityLevel.MINOR)
    public void TC_U11_PartialBody() {
        Response r1 = ApiKeyword.post(EndPointGlobal.EP_UPDATE_CART, "{}");
        Assert.assertTrue(r1.getStatusCode() < 500, "Empty body must not 5xx, got: " + r1.getStatusCode());

        Response r2 = ApiKeyword.post(EndPointGlobal.EP_UPDATE_CART, "{\"quantity_selected\":2}");
        Assert.assertTrue(r2.getStatusCode() < 500, "Missing product_id must not 5xx, got: " + r2.getStatusCode());

        Response r3 = ApiKeyword.post(EndPointGlobal.EP_UPDATE_CART, "{\"product_id\":99791}");
        Assert.assertTrue(r3.getStatusCode() < 500, "Missing qty must not 5xx, got: " + r3.getStatusCode());
    }

    // ── Security ──────────────────────────────────────────────────────────────

    @Test(priority = 12, groups = {"security"})
    @Story("TC-U12: Unauthenticated request — must not update successfully")
    @Description("Sends an update request with no session cookies. "
            + "The server must deny the operation (401/403/302) or return HTTP 200 "
            + "with error_code != 0. A successful unauthenticated update is a critical "
            + "authorisation bypass bug.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_U12_UnauthenticatedRequest() {
        Response response = RestAssured.given()
                .baseUri("https://hasaki.vn")
                .contentType("application/json")
                .header("User-Agent", "Mozilla/5.0")
                .body(buildBody(PRODUCT_VALID, 2))
                .when().post(EndPointGlobal.EP_UPDATE_CART)
                .then().extract().response();

        int status = response.getStatusCode();
        Assert.assertTrue(status == 401 || status == 403 || status == 302 || status == 200,
                "Unexpected HTTP=" + status + " for unauthenticated update");
        if (status == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "Unauthenticated request must not succeed (error_code must be != 0)");
        }
    }

    @Test(priority = 13, groups = {"security"})
    @Story("TC-U13: Invalid CSRF token — no 5xx, documents CSRF behavior")
    @Description("Sends a valid session but with a tampered form_key (CSRF token). "
            + "Related to SEC-001 — if server ignores the CSRF check, the update may succeed "
            + "even with an invalid token. Test must not 5xx; documents the finding in logs.")
    @Issue("SEC-001")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_U13_InvalidCsrfToken() {
        Response response = RestAssured.given()
                .baseUri("https://hasaki.vn")
                .contentType("application/json")
                .header("User-Agent", "Mozilla/5.0")
                .queryParam("form_key", "invalid_csrf_token_12345")
                .cookies(TokenGlobal.COOKIES != null ? TokenGlobal.COOKIES : new java.util.HashMap<>())
                .body(buildBody(PRODUCT_VALID, 2))
                .when().post(EndPointGlobal.EP_UPDATE_CART)
                .then().extract().response();

        Assert.assertTrue(response.getStatusCode() < 500,
                "Invalid CSRF token must not return 5xx, got: " + response.getStatusCode());
        Assert.assertNotNull(response.jsonPath().get("status.error_code"),
                "error_code must always be present");
    }

    // ── Integration (E2E) ─────────────────────────────────────────────────────

    @Test(priority = 14, groups = {"integration", "smoke", "e2e"})
    @Story("TC-U14: [E2E] Search → Add → Update qty — full cart update flow")
    @Description("Full cross-endpoint user journey:\n"
            + "  Step 1 — GET /search for 'SON MOI': retrieve product list, pick first id.\n"
            + "  Step 2 — POST /add-to-cart with the first product id (qty=1).\n"
            + "  Step 3 — POST /update-product to change qty to 2.\n"
            + "Asserts each step succeeds independently and the final update returns "
            + "a non-empty alert_message. Validates the add→update dependency chain.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC_U14_E2E_SearchAddThenUpdate() {
        Assert.assertNotNull(TokenGlobal.FORMKEY, "FORMKEY null — session not established");
        Assert.assertNotNull(TokenGlobal.COOKIES, "COOKIES null — session not established");

        // Step 1: search for products
        Response searchResp = ApiKeyword.get(
                EndPointGlobal.EP_SEARCH + "?keyword=SON+MOI&page=1&size=20&has_meta_data=1");
        ResponseValidator.assertHasakiSuccess(searchResp);

        List<Integer> ids = searchResp.jsonPath().getList("data.products.id");
        Assert.assertNotNull(ids, "Step1: product ids must not be null");
        Assert.assertFalse(ids.isEmpty(), "Step1: product list must not be empty");

        int targetId = ids.get(0);

        // Step 2: add to cart
        AddToCartModel addBody = AddToCartModel.builder()
                .product(AddToCartModel.ProductItem.builder()
                        .id(targetId).quantitySelected(1).giftGroupId("").build())
                .build();
        Response addResp = ApiKeyword.post(EndPointGlobal.EP_ADD_TO_CART,
                new com.google.gson.Gson().toJson(addBody));
        ResponseValidator.assertHasakiSuccess(addResp);

        // Step 3: update qty to 2
        Response updateResp = callUpdate(buildBody(targetId, 2));
        ResponseValidator.assertHasakiSuccess(updateResp);
        Assert.assertNotNull(updateResp.jsonPath().get("data"), "Step3: data block must be present");
        ResponseValidator.assertFieldNotEmpty(updateResp, "status.alert_message");

        LogUtils.info("TC-U14 E2E PASS — productId=" + targetId
                + " | alert=" + updateResp.jsonPath().getString("status.alert_message"));
    }

    // ── Performance ───────────────────────────────────────────────────────────

    @Test(priority = 15, groups = {"performance"})
    @Story("TC-U15: Response time < 3000ms")
    @Description("Cart updates are triggered by the user changing the quantity stepper. "
            + "Slow responses here cause UI spinner delays on a high-interaction component. "
            + "Must complete within 3000ms.")
    @Severity(SeverityLevel.MINOR)
    public void TC_U15_UpdateCartResponseTime() {
        ensureInCart(PRODUCT_VALID, 1);
        Response response = callUpdate(buildBody(PRODUCT_VALID, 2));
        ResponseValidator.assertStatusOk(response);
        ResponseValidator.assertResponseTimeLessThan(response, 3000);
    }

    // ── Data-driven ───────────────────────────────────────────────────────────

    @Test(priority = 20,
          dataProvider = "validUpdateCartData",
          dataProviderClass = DataProviders.class,
          groups = {"positive", "datadriven"})
    @Story("TC-U20: Data-driven — valid product/quantity combinations")
    @Description("Parameterised positive test: each row in UpdateCartData.json provides a product id "
            + "and quantity. ensureInCart() is called per row to guarantee the product is present. "
            + "Every combination must return error_code=0 with a non-null data block.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_U20_DataDriven_ValidData(
            String tcId, int productId, int qty, String description) {

        LogUtils.info("[" + tcId + "] " + description + " | productId=" + productId + ", qty=" + qty);
        ensureInCart(productId, 1);

        Response response = callUpdate(buildBody(productId, qty));
        ResponseValidator.assertHasakiSuccess(response);
        Assert.assertNotNull(response.jsonPath().get("data"), tcId + ": data block must be present");
    }

    @Test(priority = 21,
          dataProvider = "invalidUpdateCartData",
          dataProviderClass = DataProviders.class,
          groups = {"negative", "datadriven"})
    @Story("TC-U21: Data-driven — invalid combinations — error_code != 0, no 5xx")
    @Description("Parameterised negative test: each row in UpdateCartInvalidData.json contains "
            + "an invalid combination (non-existent product, negative qty, etc.). "
            + "Every row must return HTTP < 500; if HTTP 200, error_code must be non-zero "
            + "and error_message must be non-empty.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_U21_DataDriven_InvalidData(
            String tcId, int productId, int qty, String description) {

        LogUtils.info("[" + tcId + "] " + description + " | productId=" + productId + ", qty=" + qty);

        Response response = callUpdate(buildBody(productId, qty));

        Assert.assertTrue(response.getStatusCode() < 500,
                tcId + ": must not return 5xx. HTTP=" + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    tcId + ": error_code must be non-zero for invalid input");
            ResponseValidator.assertFieldNotEmpty(response, "status.error_message");
        }
    }
}
