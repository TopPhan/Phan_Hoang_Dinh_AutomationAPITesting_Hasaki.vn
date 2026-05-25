package com.testScenarios;

import com.baseSetup.BaseTest;
import com.globals.EndPointGlobal;
import com.helper.LogUtils;
import com.helper.SchemaHelper;
import com.keywords.ApiKeyword;
import com.pojoModel.AddToCartModel;
import com.validator.ResponseValidator;
import dataProvider.DataProviders;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * AddToCartTest — Add Product to Cart feature
 * Endpoint: POST /mobile/v1/checkout/cart/add-to-cart
 *
 * Test flow order:
 *   [1] Positive     — valid product, alert message content, duplicate add (idempotency)
 *   [2] Negative     — negative product_id, non-existent product, qty=0, qty=-1, empty body
 *   [3] Integration  — E2E flow: Filter → Search → Add first result to cart
 *   [4] Schema       — response contract validation
 *   [5] Performance  — response time threshold
 *   [6] Data-driven  — bulk valid and invalid product/quantity combinations
 *
 * Pre-condition: requires valid session (TokenGlobal.COOKIES + FORMKEY).
 *   BaseTest.loginAndGetSession() ensures session is present before each class.
 *
 * Request body:
 *   product.id                — product ID to add
 *   product.quantity_selected — quantity to add
 *   product.gift_group_id     — gift group (empty string for regular products)
 *
 * Response structure (success):
 *   status.error_code    — 0 = success
 *   status.alert_message — user-facing confirmation message
 *   data.cart_id         — cart identifier (> 0)
 *   data.products_total  — total distinct products in cart after add (>= 1)
 *
 * Note: TC-C20/C21 (data-driven) cover individual product/qty combos in bulk,
 *   so standalone positive tests focus on business-logic assertions not covered there.
 */
@Epic("Hasaki.vn API Testing")
@Feature("Add To Cart")
public class AddToCartTest extends BaseTest {

    private static final int PRODUCT_VALID    = 6710;
    private static final int PRODUCT_INVALID  = -999;
    private static final int PRODUCT_NONEXIST = 9999999;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a standard add-to-cart request body.
     *
     * @param productId product ID to add
     * @param qty       quantity to add
     * @return populated AddToCartModel ready for serialisation
     */
    private AddToCartModel buildBody(int productId, int qty) {
        return AddToCartModel.builder()
                .product(AddToCartModel.ProductItem.builder()
                        .id(productId)
                        .quantitySelected(qty)
                        .giftGroupId("")
                        .build())
                .build();
    }

    /**
     * Serialises the body to JSON, logs it, and posts to the add-to-cart endpoint.
     *
     * @param body request body object (AddToCartModel or raw JSON string)
     * @return API response
     */
    private Response callAdd(Object body) {
        String json = new com.google.gson.Gson().toJson(body);
        LogUtils.info("AddToCart body: " + json);
        return ApiKeyword.post(EndPointGlobal.EP_ADD_TO_CART, json);
    }

    // ── Positive ──────────────────────────────────────────────────────────────

    @Test(priority = 1, groups = {"positive", "smoke"})
    @Story("TC-C01: Add valid product — cart_id > 0, products_total >= 1")
    @Description("Core smoke test: adding a known valid product with qty=1 must return "
            + "error_code=0, a positive cart_id, and products_total >= 1. "
            + "This is the fundamental happy-path assertion for the entire cart feature.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC_C01_AddValidProduct() {
        Response response = callAdd(buildBody(PRODUCT_VALID, 1));

        ResponseValidator.assertHasakiSuccess(response);

        Long cartId = response.jsonPath().get("data.cart_id");
        Assert.assertNotNull(cartId, "cart_id must not be null");
        Assert.assertTrue(cartId > 0, "cart_id must be > 0, got: " + cartId);

        Integer total = response.jsonPath().get("data.products_total");
        Assert.assertNotNull(total, "products_total must not be null");
        Assert.assertTrue(total >= 1, "products_total must be >= 1 after add, got: " + total);

        LogUtils.info("TC-C01 PASS — cart_id=" + cartId + ", products_total=" + total);
    }

    @Test(priority = 2, groups = {"positive"})
    @Story("TC-C02: Success alert_message is non-blank and indicates success")
    @Description("Verifies the user-facing confirmation message is present and contains "
            + "success-indicating text ('thành công', 'added', or 'success'). "
            + "A blank or missing alert_message would leave the user without visual feedback.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_C02_SuccessAlertMessage() {
        Response response = callAdd(buildBody(PRODUCT_VALID, 1));
        ResponseValidator.assertHasakiSuccess(response);

        String msg = response.jsonPath().getString("status.alert_message");
        Assert.assertNotNull(msg, "alert_message must not be null");
        Assert.assertFalse(msg.isBlank(), "alert_message must not be blank");

        String lower = msg.toLowerCase();
        Assert.assertTrue(lower.contains("thành công") || lower.contains("added") || lower.contains("success"),
                "alert_message does not indicate success, got: '" + msg + "'");
    }

    @Test(priority = 3, groups = {"positive"})
    @Story("TC-C03: Adding same product twice — products_total must not decrease")
    @Description("When the same product is added a second time, the server may either merge "
            + "the quantity (products_total stays the same) or treat it as a second line item "
            + "(products_total increments). Either behaviour is acceptable, but products_total "
            + "must never decrease — that would indicate a cart state corruption bug.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_C03_AddSameProductTwiceCartTotalNonDecreasing() {
        Response first = callAdd(buildBody(PRODUCT_VALID, 1));
        ResponseValidator.assertHasakiSuccess(first);
        Integer totalAfterFirst = first.jsonPath().get("data.products_total");
        Assert.assertNotNull(totalAfterFirst, "products_total must not be null after first add");

        Response second = callAdd(buildBody(PRODUCT_VALID, 1));
        ResponseValidator.assertHasakiSuccess(second);
        Integer totalAfterSecond = second.jsonPath().get("data.products_total");
        Assert.assertNotNull(totalAfterSecond, "products_total must not be null after second add");

        Assert.assertTrue(totalAfterSecond >= totalAfterFirst,
                "products_total must not decrease after second add. before="
                + totalAfterFirst + ", after=" + totalAfterSecond);
    }

    // ── Negative ──────────────────────────────────────────────────────────────

    @Test(priority = 4, groups = {"negative", "smoke"})
    @Story("TC-C04: Negative product_id — error_code != 0, no cart_id")
    @Description("product_id=-999 is an impossible value. The server must reject it "
            + "with error_code != 0 and a non-empty error_message. "
            + "A 5xx here would indicate the server is not sanitising the input.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_C04_NegativeProductId() {
        Response response = callAdd(buildBody(PRODUCT_INVALID, 1));

        Assert.assertTrue(response.getStatusCode() < 500,
                "Must not return 5xx for negative product_id, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero for product_id=-999");
            ResponseValidator.assertFieldNotEmpty(response, "status.error_message");
        }
    }

    @Test(priority = 5, groups = {"negative"})
    @Story("TC-C05: Non-existent product_id — error_code != 0")
    @Description("product_id=9999999 is a positive integer but does not exist in the catalogue. "
            + "Server must look it up and return a meaningful error — not silently succeed "
            + "or crash with a 5xx.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_C05_NonExistentProductId() {
        Response response = callAdd(buildBody(PRODUCT_NONEXIST, 1));

        Assert.assertTrue(response.getStatusCode() < 500,
                "Must not return 5xx for non-existent product, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero for non-existent product_id=" + PRODUCT_NONEXIST);
        }
        LogUtils.info("TC-C05 PASS — HTTP=" + response.getStatusCode()
                + " | error_code=" + response.jsonPath().get("status.error_code"));
    }

    @Test(priority = 6, groups = {"negative"})
    @Story("TC-C06: qty=0 — server must reject or return error_code != 0")
    @Description("Quantity zero has no business meaning in a cart. "
            + "The server should either reject the request outright (4xx) "
            + "or return HTTP 200 with error_code != 0.")
    @Severity(SeverityLevel.MINOR)
    public void TC_C06_QuantityZero() {
        Response response = callAdd(buildBody(PRODUCT_VALID, 0));

        Assert.assertTrue(response.getStatusCode() < 500,
                "qty=0 must not return 5xx, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero for qty=0");
        }
    }

    @Test(priority = 7, groups = {"negative"})
    @Story("TC-C07: Negative quantity — server must reject or return error_code != 0")
    @Description("quantity_selected=-1 is an invalid boundary value. "
            + "Server must not add a negative quantity to the cart "
            + "or produce a 5xx from an unhandled arithmetic operation.")
    @Severity(SeverityLevel.MINOR)
    public void TC_C07_NegativeQuantity() {
        Response response = callAdd(buildBody(PRODUCT_VALID, -1));

        Assert.assertTrue(response.getStatusCode() < 500,
                "Negative qty must not return 5xx, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero for qty=-1");
        }
    }

    @Test(priority = 8, groups = {"negative"})
    @Story("TC-C08: Empty body — server rejects gracefully, no 5xx")
    @Description("Sends an empty JSON object '{}' — no product or quantity fields. "
            + "Verifies the server performs required-field validation and returns "
            + "a client error or error_code != 0, never an unhandled 5xx.")
    @Severity(SeverityLevel.MINOR)
    public void TC_C08_EmptyBody() {
        Response response = ApiKeyword.post(EndPointGlobal.EP_ADD_TO_CART, "{}");

        Assert.assertTrue(response.getStatusCode() < 500,
                "Empty body must not return 5xx, got: " + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    "error_code must be non-zero for empty body");
        }
    }

    // ── Integration (E2E) ─────────────────────────────────────────────────────

    @Test(priority = 9, groups = {"integration", "smoke", "e2e"})
    @Story("TC-C09: [E2E] Filter → Search → Add first result to cart")
    @Description("Full user journey through three endpoints:\n"
            + "  Step 1 — GET /filters for 'SON MOI': asserts filters are returned.\n"
            + "  Step 2 — GET /search for 'SON MOI': retrieves product list and picks first id.\n"
            + "  Step 3 — POST /add-to-cart with the first search result id.\n"
            + "Validates the complete upstream dependency chain — a search-result product id "
            + "must be valid and addable to cart without any intermediate manual step.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC_C09_E2E_FilterSearchAddToCart() {
        final String keyword = "SON MOI";

        // Step 1: filters must return results
        Response filterResp = ApiKeyword.get(
                EndPointGlobal.EP_FILTER + "?q=" + keyword.replace(" ", "+") + "&platform=web");
        ResponseValidator.assertHasakiSuccess(filterResp);
        List<Map<String, Object>> filters = filterResp.jsonPath().getList("data.filter");
        Assert.assertNotNull(filters, "Step1: data.filter must not be null");
        Assert.assertFalse(filters.isEmpty(), "Step1: at least 1 filter expected");

        // Step 2: search returns products
        Response searchResp = ApiKeyword.get(
                EndPointGlobal.EP_SEARCH + "?keyword=" + keyword.replace(" ", "+")
                + "&page=1&size=20&has_meta_data=1");
        ResponseValidator.assertHasakiSuccess(searchResp);
        List<Integer> productIds = searchResp.jsonPath().getList("data.products.id");
        Assert.assertNotNull(productIds, "Step2: product ids must not be null");
        Assert.assertFalse(productIds.isEmpty(), "Step2: product list must not be empty");

        // Step 3: add first product to cart
        int firstId = productIds.get(0);
        Response cartResp = callAdd(buildBody(firstId, 1));
        ResponseValidator.assertHasakiSuccess(cartResp);

        Long cartId = cartResp.jsonPath().get("data.cart_id");
        Assert.assertNotNull(cartId, "Step3: cart_id must not be null");
        Assert.assertTrue(cartId > 0, "Step3: cart_id must be > 0");

        LogUtils.info("TC-C09 E2E PASS — filters=" + filters.size()
                + " → productId=" + firstId + " → cart_id=" + cartId);
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    @Test(priority = 10, groups = {"schema"})
    @Story("TC-C10: Add-to-cart response schema validation")
    @Description("Validates the success response structure against AddToCartSchema.json. "
            + "Ensures field names, types (cart_id as long, products_total as int), "
            + "and required fields are preserved across deployments.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_C10_AddToCartResponseSchema() {
        Response response = callAdd(buildBody(PRODUCT_VALID, 1));
        ResponseValidator.assertStatusOk(response);
        SchemaHelper.verifySchema(response, "jsonSchema/AddToCartSchema.json");
    }

    // ── Performance ───────────────────────────────────────────────────────────

    @Test(priority = 11, groups = {"performance"})
    @Story("TC-C11: Response time < 3000ms")
    @Description("Adding to cart is a transactional write operation triggered by user intent. "
            + "Latency above 3000ms causes perceived cart abandonment and is a regression indicator.")
    @Severity(SeverityLevel.MINOR)
    public void TC_C11_AddToCartResponseTime() {
        Response response = callAdd(buildBody(PRODUCT_VALID, 1));
        ResponseValidator.assertStatusOk(response);
        ResponseValidator.assertResponseTimeLessThan(response, 3000);
    }

    // ── Data-driven ───────────────────────────────────────────────────────────

    @Test(priority = 20,
          dataProvider = "validAddToCartData",
          dataProviderClass = DataProviders.class,
          groups = {"positive", "datadriven"})
    @Story("TC-C20: Data-driven — valid product/quantity combinations")
    @Description("Parameterised positive test: each row in AddToCartData.json provides a product id "
            + "and quantity. Every combination must return error_code=0, a positive cart_id, "
            + "and products_total >= 1. Covers multiple product types and boundary quantities.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_C20_DataDriven_ValidData(
            String tcId, int productId, int qty, String description) {

        LogUtils.info("[" + tcId + "] " + description + " | productId=" + productId + ", qty=" + qty);

        Response response = callAdd(buildBody(productId, qty));
        ResponseValidator.assertHasakiSuccess(response);

        Long cartId = response.jsonPath().get("data.cart_id");
        Assert.assertNotNull(cartId, tcId + ": cart_id must not be null");
        Assert.assertTrue(cartId > 0, tcId + ": cart_id must be > 0");

        Integer total = response.jsonPath().get("data.products_total");
        Assert.assertNotNull(total, tcId + ": products_total must not be null");
        Assert.assertTrue(total >= 1, tcId + ": products_total must be >= 1");
    }

    @Test(priority = 21,
          dataProvider = "invalidAddToCartData",
          dataProviderClass = DataProviders.class,
          groups = {"negative", "datadriven"})
    @Story("TC-C21: Data-driven — invalid product/quantity — error_code != 0, no 5xx")
    @Description("Parameterised negative test: each row in AddToCartInvalidData.json contains "
            + "an invalid combination (non-existent id, zero qty, negative qty, etc.). "
            + "Every row must produce HTTP < 500; if HTTP 200, error_code must be non-zero "
            + "and error_message must be non-empty.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_C21_DataDriven_InvalidData(
            String tcId, int productId, int qty, String description) {

        LogUtils.info("[" + tcId + "] " + description + " | productId=" + productId + ", qty=" + qty);

        Response response = callAdd(buildBody(productId, qty));

        Assert.assertTrue(response.getStatusCode() < 500,
                tcId + ": must not return 5xx. HTTP=" + response.getStatusCode());
        if (response.getStatusCode() == 200) {
            Assert.assertNotEquals(response.jsonPath().getInt("status.error_code"), 0,
                    tcId + ": error_code must be non-zero for invalid input");
            ResponseValidator.assertFieldNotEmpty(response, "status.error_message");
        }
    }
}
