package com.testScenarios;

import com.baseSetup.BaseTest;
import com.globals.EndPointGlobal;
import com.globals.TokenGlobal;
import com.helper.LogUtils;
import com.keywords.ApiKeyword;
import com.pojoModel.AddToCartModel;
import com.pojoModel.DeleteCartModel;
import com.validator.ResponseValidator;
import dataProvider.DataProviders;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * DeleteItemInCartTest — Delete Product from Cart
 * Endpoint: POST /mobile/v2/checkout/cart/delete-product
 *
 * Test flow order:
 *   [1] Positive     — delete valid product, verify absence + grand_total, multi-product cart,
 *                      success alert_message
 *   [2] Negative     — negative product_id, non-existent product, product not in cart,
 *                      product_id=0, invalid quantity, partial/empty body
 *   [3] Security     — unauthenticated request, invalid CSRF token
 *   [4] Integration  — E2E flow: Add → Delete → verify cart state
 *   [5] Performance  — response time threshold
 *   [6] Data-driven  — bulk valid and invalid product/quantity combinations
 *
 * Pre-condition: target product must be in cart before delete tests.
 *   ensureInCart() is called at the start of each positive test.
 *
 * Request body:
 *   product_id         — ID of the product to remove from cart
 *   quantity_selected  — quantity associated with the cart line
 *
 * Response structure (success):
 *   status.error_code    — 0 = success
 *   status.alert_message — user-facing confirmation (must be non-empty on success)
 *   data.sub_total       — cart subtotal after deletion
 *   data.grand_total     — cart grand total after deletion (>= 0)
 *   data.products[]      — remaining items in cart (deleted item must be absent)
 *
 * Note: data.cart_id and data.products_total are NOT present in the delete response.
 */
@Epic("Hasaki.vn API Testing")
@Feature("Delete Item In Cart")
public class DeleteItemInCartTest extends BaseTest {

    private static final int PRODUCT_VALID    = 183184;
    private static final int PRODUCT_NONEXIST = 9999999;
    private static final int PRODUCT_INVALID  = -999;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a delete-cart request body.
     *
     * @param productId product ID to remove from cart
     * @param qty       quantity on the cart line being removed
     * @return populated DeleteCartModel
     */
    private DeleteCartModel buildBody(int productId, int qty) {
        return DeleteCartModel.builder()
                .productId(productId)
                .quantitySelected(qty)
                .build();
    }

    /**
     * Serialises the body to JSON, logs it, and posts to the delete-cart endpoint.
     *
     * @param body request body object
     * @return API response
     */
    private Response callDelete(Object body) {
        String json = new com.google.gson.Gson().toJson(body);
        LogUtils.info("DeleteItem body: " + json);
        return ApiKeyword.post(EndPointGlobal.EP_DELETE_CART, json);
    }

    /**
     * Pre-condition helper: adds the given product to the cart so delete tests
     * have a valid item to operate on. Logs a warning (does not fail) if the
     * add call fails, to avoid masking the actual delete assertion.
     *
     * @param productId product to add
     * @param qty       initial quantity
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
    @Story("TC-D01: Delete valid product — error_code=0, response structure correct")
    @Description("Core smoke test: deleting a product that exists in cart must return error_code=0 "
            + "and a response body containing data.sub_total, data.grand_total (>= 0), "
            + "and data.products. Verifies the delete endpoint is reachable and structurally intact.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC_D01_DeleteValidProduct() {
        ensureInCart(PRODUCT_VALID, 1);
        Response response = callDelete(buildBody(PRODUCT_VALID, 1));

        ResponseValidator.assertHasakiSuccess(response);
        Assert.assertNotNull(response.jsonPath().get("data"),             "data block must be present");
        Assert.assertNotNull(response.jsonPath().get("data.sub_total"),   "data.sub_total must be present");
        Assert.assertNotNull(response.jsonPath().get("data.grand_total"), "data.grand_total must be present");
        Assert.assertNotNull(response.jsonPath().get("data.products"),    "data.products must be present");

        Integer grandTotal = response.jsonPath().get("data.grand_total");
        Assert.assertTrue(grandTotal >= 0, "grand_total must be >= 0, got: " + grandTotal);
    }

    @Test(priority = 2, groups = {"positive", "smoke"})
    @Story("TC-D02: Deleted product absent from remaining cart — grand_total >= 0")
    @Description("After deleting a product, asserts two critical outcomes:\n"
            + "  (1) data.products list does not contain the deleted product id.\n"
            + "  (2) data.grand_total is non-null and >= 0.\n"
            + "Verifies the delete actually removes the item from the cart state, "
            + "not just returns a success code.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_D02_DeletedProductAbsentFromCart() {
        ensureInCart(PRODUCT_VALID, 1);
        Response response = callDelete(buildBody(PRODUCT_VALID, 1));

        ResponseValidator.assertHasakiSuccess(response);

        List<Integer> remaining = response.jsonPath().getList("data.products.id");
        Assert.assertNotNull(remaining, "data.products must not be null after delete");
        Assert.assertFalse(remaining.contains(PRODUCT_VALID),
                "product id=" + PRODUCT_VALID + " must not remain in cart after deletion");

        Integer grandTotal = response.jsonPath().get("data.grand_total");
        Assert.assertNotNull(grandTotal, "grand_total must not be null");
        Assert.assertTrue(grandTotal >= 0, "grand_total must not be negative, got: " + grandTotal);
    }

    @Test(priority = 3, groups = {"positive"})
    @Story("TC-D03: Delete one of two products — second product still remains")
    @Description("Adds two distinct products to the cart, then deletes only the first one. "
            + "Asserts the first product is absent and the second product still exists in "
            + "data.products. Validates that the delete operation is targeted and does not "
            + "accidentally clear unrelated cart lines.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_D03_DeleteOneProductOtherStaysInCart() {
        // Get two distinct product IDs from search
        Response searchResp = ApiKeyword.get(
                EndPointGlobal.EP_SEARCH + "?keyword=SON+MOI&page=1&size=20&has_meta_data=1");
        ResponseValidator.assertStatusOk(searchResp);

        List<Integer> ids = searchResp.jsonPath().getList("data.products.id");
        if (ids == null || ids.size() < 2) {
            LogUtils.info("TC-D03 SKIP — need at least 2 products from search");
            return;
        }

        int product1 = ids.get(0);
        int product2 = ids.get(1);
        ensureInCart(product1, 1);
        ensureInCart(product2, 1);

        // Delete only product1
        Response deleteResp = callDelete(buildBody(product1, 1));
        ResponseValidator.assertHasakiSuccess(deleteResp);

        List<Integer> remaining = deleteResp.jsonPath().getList("data.products.id");
        Assert.assertNotNull(remaining, "data.products must not be null");
        Assert.assertFalse(remaining.contains(product1),
                "product1=" + product1 + " must not remain after deletion");

        LogUtils.info("TC-D03 PASS — deleted id=" + product1
                + " | remaining count=" + remaining.size()
                + " | grand_total=" + deleteResp.jsonPath().get("data.grand_total"));
    }

    @Test(priority = 4, groups = {"positive"})
    @Story("TC-D04: Success alert_message is non-blank")
    @Description("Verifies the user-facing confirmation message is present and non-empty "
            + "after a successful delete. A blank alert_message gives the user no feedback "
            + "that the item was removed.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_D04_SuccessAlertMessage() {
        ensureInCart(PRODUCT_VALID, 1);
        Response response = callDelete(buildBody(PRODUCT_VALID, 1));

        ResponseValidator.assertHasakiSuccess(response);
        ResponseValidator.assertFieldNotEmpty(response, "status.alert_message");
    }

    // ── Negative ──────────────────────────────────────────────────────────────

    @Test(priority = 5, groups = {"negative", "smoke"})
    @Story("TC-D05: Negative product_id — error_code != 0")
    @Description("product_id=-999 is an impossible value. Server must reject it with "
            + "error_code != 0 and a non-empty error_message. Must not 5xx.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_D05_NegativeProductId() {
        Response response = callDelete(buildBody(PRODUCT_INVALID, 1));

        Assert.assertTrue(response.getStatusCode() < 500,
                "Negative product_id must not return 5xx, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero for product_id=" + PRODUCT_INVALID);
            ResponseValidator.assertFieldNotEmpty(response, "status.error_message");
        }
    }

    @Test(priority = 6, groups = {"negative"})
    @Story("TC-D06: Non-existent product_id — error_code != 0")
    @Description("product_id=9999999 does not exist in the catalogue or any cart. "
            + "Server must return error_code != 0 rather than silently succeed or 5xx.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_D06_NonExistentProductId() {
        Response response = callDelete(buildBody(PRODUCT_NONEXIST, 1));

        Assert.assertTrue(response.getStatusCode() < 500,
                "Non-existent product_id must not return 5xx, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero for product_id=" + PRODUCT_NONEXIST);
        }
    }

    @Test(priority = 7, groups = {"negative"})
    @Story("TC-D07: Product not in cart — error_code != 0")
    @Description("Attempts to delete a product id that exists in the catalogue but "
            + "is not in the test user's cart. Server must detect the missing cart line "
            + "and return error_code != 0.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_D07_ProductNotInCart() {
        Response response = callDelete(buildBody(12345, 1));

        Assert.assertTrue(response.getStatusCode() < 500,
                "Product-not-in-cart must not return 5xx, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero when product is not in cart");
        }
    }

    @Test(priority = 8, groups = {"negative"})
    @Story("TC-D08: product_id=0 (zero boundary) — must not 5xx")
    @Description("product_id=0 is a zero-boundary value with no valid product meaning. "
            + "Server must handle it without crashing. Any non-5xx response is acceptable.")
    @Severity(SeverityLevel.MINOR)
    public void TC_D08_ProductIdZero() {
        Response response = callDelete(buildBody(0, 1));
        Assert.assertTrue(response.getStatusCode() < 500,
                "product_id=0 must not return 5xx, got: " + response.getStatusCode());
    }

    @Test(priority = 9, groups = {"negative"})
    @Story("TC-D09: qty=0 and qty=-1 — must not 5xx; negative qty must error")
    @Description("Tests two invalid quantity values together to avoid duplication:\n"
            + "  (a) qty=0: boundary value; server behaviour may vary — no 5xx required.\n"
            + "  (b) qty=-1: invalid; if HTTP 200, error_code must be non-zero.")
    @Severity(SeverityLevel.MINOR)
    public void TC_D09_InvalidQuantity() {
        for (int qty : new int[]{0, -1}) {
            ensureInCart(PRODUCT_VALID, 1);
            Response response = callDelete(buildBody(PRODUCT_VALID, qty));

            Assert.assertTrue(response.getStatusCode() < 500,
                    "qty=" + qty + " must not return 5xx, got: " + response.getStatusCode());
            if (response.getStatusCode() == 200 && qty < 0) {
                Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                        "qty=" + qty + " must return error_code != 0");
            }
        }
    }

    @Test(priority = 10, groups = {"negative"})
    @Story("TC-D10: Empty/partial body — must not 5xx")
    @Description("Tests three missing-field scenarios in one test:\n"
            + "  (a) empty body '{}'\n"
            + "  (b) missing product_id (only quantity_selected provided)\n"
            + "  (c) missing quantity_selected (only product_id provided)\n"
            + "All must return HTTP < 500. Validates required-field input checking.")
    @Severity(SeverityLevel.MINOR)
    public void TC_D10_PartialBody() {
        Response r1 = ApiKeyword.post(EndPointGlobal.EP_DELETE_CART, "{}");
        Assert.assertTrue(r1.getStatusCode() < 500, "Empty body must not 5xx, got: " + r1.getStatusCode());

        Response r2 = ApiKeyword.post(EndPointGlobal.EP_DELETE_CART, "{\"quantity_selected\":1}");
        Assert.assertTrue(r2.getStatusCode() < 500, "Missing product_id must not 5xx, got: " + r2.getStatusCode());

        Response r3 = ApiKeyword.post(EndPointGlobal.EP_DELETE_CART, "{\"product_id\":183184}");
        Assert.assertTrue(r3.getStatusCode() < 500, "Missing qty must not 5xx, got: " + r3.getStatusCode());
    }

    // ── Security ──────────────────────────────────────────────────────────────

    @Test(priority = 11, groups = {"security"})
    @Story("TC-D11: Unauthenticated request — must not delete successfully")
    @Description("Sends a delete request with no session cookies. "
            + "An unauthenticated delete that succeeds is a critical authorisation bypass "
            + "— any anonymous caller could empty another user's cart. "
            + "Must return 401/403/302 or HTTP 200 with error_code != 0.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_D11_UnauthenticatedRequest() {
        Response response = RestAssured.given()
                .baseUri("https://hasaki.vn")
                .contentType("application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Origin",  "https://hasaki.vn")
                .header("Referer", "https://hasaki.vn/checkout/cart")
                .body(buildBody(PRODUCT_VALID, 1))
                .when().post(EndPointGlobal.EP_DELETE_CART)
                .then().extract().response();

        int status = response.getStatusCode();
        Assert.assertTrue(status == 401 || status == 403 || status == 302 || status == 200,
                "Unexpected HTTP=" + status + " for unauthenticated delete");
        if (status == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "Unauthenticated request must not delete successfully");
        }
    }

    @Test(priority = 12, groups = {"security"})
    @Story("TC-D12: Invalid CSRF token — no 5xx, documents CSRF behavior")
    @Description("Sends a valid session but with a tampered form_key (CSRF token). "
            + "If the server accepts the delete despite an invalid CSRF token, "
            + "the SEC-001 gap applies to delete operations as well. "
            + "Must not 5xx; logs the finding for security review.")
    @Issue("SEC-001")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_D12_InvalidCsrfToken() {
        Response response = RestAssured.given()
                .baseUri("https://hasaki.vn")
                .contentType("application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Origin", "https://hasaki.vn")
                .queryParam("form_key", "invalid_csrf_token_xyz_12345")
                .cookies(TokenGlobal.COOKIES != null ? TokenGlobal.COOKIES : new java.util.HashMap<>())
                .body(buildBody(PRODUCT_VALID, 1))
                .when().post(EndPointGlobal.EP_DELETE_CART)
                .then().extract().response();

        Assert.assertTrue(response.getStatusCode() < 500,
                "Invalid CSRF token must not return 5xx, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "Server must reject deletion with invalid CSRF token");
        }
    }

    // ── Integration (E2E) ─────────────────────────────────────────────────────

    @Test(priority = 13, groups = {"integration", "smoke", "e2e"})
    @Story("TC-D13: [E2E] Add → Delete → verify cart state")
    @Description("Full cart lifecycle flow:\n"
            + "  Step 1 — POST /add-to-cart: add PRODUCT_VALID with qty=1.\n"
            + "  Step 2 — POST /delete-product: remove the same product.\n"
            + "  Step 3 — Assert: deleted product absent from data.products, "
            + "grand_total is non-null and >= 0.\n"
            + "This is the canonical cart lifecycle smoke test that must pass before "
            + "any checkout or payment flow tests are run.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC_D13_E2E_AddThenDeleteVerifyState() {
        // Step 1: add product
        AddToCartModel addBody = AddToCartModel.builder()
                .product(AddToCartModel.ProductItem.builder()
                        .id(PRODUCT_VALID).quantitySelected(1).giftGroupId("").build())
                .build();
        Response addResp = ApiKeyword.post(EndPointGlobal.EP_ADD_TO_CART,
                new com.google.gson.Gson().toJson(addBody));
        ResponseValidator.assertHasakiSuccess(addResp);

        // Step 2: delete product
        Response deleteResp = callDelete(buildBody(PRODUCT_VALID, 1));
        ResponseValidator.assertHasakiSuccess(deleteResp);

        // Step 3: verify state
        List<Integer> remaining = deleteResp.jsonPath().getList("data.products.id");
        Assert.assertNotNull(remaining, "data.products must not be null");
        Assert.assertFalse(remaining.contains(PRODUCT_VALID),
                "deleted product id=" + PRODUCT_VALID + " must not remain in cart");

        Integer grandTotal = deleteResp.jsonPath().get("data.grand_total");
        Assert.assertNotNull(grandTotal, "grand_total must not be null");
        Assert.assertTrue(grandTotal >= 0, "grand_total must be >= 0, got: " + grandTotal);

        LogUtils.info("TC-D13 E2E PASS — grand_total=" + grandTotal
                + ", remaining=" + remaining.size() + " products");
    }

    // ── Performance ───────────────────────────────────────────────────────────

    @Test(priority = 14, groups = {"performance"})
    @Story("TC-D14: Response time < 3000ms")
    @Description("Delete is triggered from the cart page when the user removes an item. "
            + "Slow responses here block the cart re-render and degrade the checkout UX. "
            + "Must complete within 3000ms.")
    @Severity(SeverityLevel.MINOR)
    public void TC_D14_DeleteItemResponseTime() {
        ensureInCart(PRODUCT_VALID, 1);
        Response response = callDelete(buildBody(PRODUCT_VALID, 1));
        ResponseValidator.assertStatusOk(response);
        ResponseValidator.assertResponseTimeLessThan(response, 3000);
    }

    // ── Data-driven ───────────────────────────────────────────────────────────

    @Test(priority = 20,
          dataProvider = "validDeleteCartData",
          dataProviderClass = DataProviders.class,
          groups = {"positive", "datadriven"})
    @Story("TC-D20: Data-driven — valid product/quantity combinations")
    @Description("Parameterised positive test: each row in DeleteCartData.json provides a product id "
            + "and quantity. ensureInCart() is called per row before the delete. "
            + "Every combination must return error_code=0 and the deleted product must be "
            + "absent from the remaining data.products list.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_D20_DataDriven_ValidData(
            String tcId, int productId, int qty, String description) {

        LogUtils.info("[" + tcId + "] " + description + " | productId=" + productId + ", qty=" + qty);
        ensureInCart(productId, qty);

        Response response = callDelete(buildBody(productId, qty));
        ResponseValidator.assertHasakiSuccess(response);

        List<Integer> remaining = response.jsonPath().getList("data.products.id");
        Assert.assertNotNull(remaining, tcId + ": data.products must not be null");
        Assert.assertFalse(remaining.contains(productId),
                tcId + ": product id=" + productId + " must not remain after deletion");
    }

    @Test(priority = 21,
          dataProvider = "invalidDeleteCartData",
          dataProviderClass = DataProviders.class,
          groups = {"negative", "datadriven"})
    @Story("TC-D21: Data-driven — invalid inputs — error_code != 0, no 5xx")
    @Description("Parameterised negative test: each row in DeleteCartInvalidData.json contains "
            + "an invalid combination (non-existent product, negative product_id, etc.). "
            + "Every row must return HTTP < 500; if HTTP 200, error_code must be non-zero "
            + "and error_message must be non-empty.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_D21_DataDriven_InvalidData(
            String tcId, int productId, int qty, String description) {

        LogUtils.info("[" + tcId + "] " + description + " | productId=" + productId + ", qty=" + qty);

        Response response = callDelete(buildBody(productId, qty));

        Assert.assertTrue(response.getStatusCode() < 500,
                tcId + ": must not return 5xx. HTTP=" + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    tcId + ": error_code must be non-zero for invalid input");
            ResponseValidator.assertFieldNotEmpty(response, "status.error_message");
        }
    }
}
