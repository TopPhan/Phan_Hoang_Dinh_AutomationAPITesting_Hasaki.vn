package com.testScenarios;

import com.baseSetup.BaseTest;
import com.globals.EndPointGlobal;
import com.helper.LogUtils;
import com.helper.SchemaHelper;
import com.keywords.ApiKeyword;
import com.validator.ResponseValidator;
import dataProvider.DataProviders;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * SearchTest — Product Search feature
 * Endpoint: GET /mobile/v1/main/search
 *
 * Test flow order:
 *   [1] Positive     — valid keywords, product field integrity, meta fields, pagination, page size, unicode
 *   [2] Negative     — non-existent keyword, empty keyword, invalid pagination params
 *   [3] Schema       — response contract validation
 *   [4] Performance  — response time threshold
 *   [5] Data-driven  — bulk valid keywords and bulk invalid keywords from JSON files
 *
 * Query params:
 *   keyword        — search term (URL-encoded)
 *   page           — page number (1-based)
 *   size           — items per page
 *   has_meta_data  — 1 = include meta_data block; 0 = omit it
 *
 * Response structure:
 *   status.error_code              — 0 = success
 *   data.meta_data.products_total  — total matching products (present when has_meta_data=1)
 *   data.meta_data.sort_params[]   — available sort options
 *   data.products[].id             — product ID (> 0)
 *   data.products[].sku            — product SKU
 *   data.products[].name           — product name
 *   data.products[].price          — product price
 *   data.products[].image          — product image URL
 *   data.is_redirect               — redirect flag
 */
@Epic("Hasaki.vn API Testing")
@Feature("Search")
public class SearchTest extends BaseTest {

    private static final String KW_VALID   = "SON MOI";
    private static final String KW_UNICODE = "son môi";
    private static final String KW_NONE    = "xyzabc123nonexistent999";
    private static final int    PAGE       = 1;
    private static final int    SIZE       = 20;

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Builds the full search request path with all required query params.
     *
     * @param keyword URL-encoded search term
     * @param page    1-based page number
     * @param size    number of items per page
     * @param meta    1 = include meta_data block, 0 = omit it
     * @return relative path ready for ApiKeyword.get()
     */
    private String buildPath(String keyword, int page, int size, int meta) {
        return EndPointGlobal.EP_SEARCH
                + "?keyword=" + keyword
                + "&page=" + page + "&size=" + size + "&has_meta_data=" + meta;
    }

    // ── Positive ──────────────────────────────────────────────────────────────

    @Test(priority = 1, groups = {"positive", "smoke"})
    @Story("TC-S01: Valid keyword — products returned")
    @Description("Core smoke test: searching with a known valid keyword must return "
            + "error_code=0, products_total > 0, a non-empty product list, "
            + "and a list size that does not exceed the requested page size.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC_S01_SearchWithValidKeyword() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PAGE, SIZE, 1));

        ResponseValidator.assertHasakiSuccess(response);

        Integer total = response.jsonPath().get("data.meta_data.products_total");
        Assert.assertNotNull(total, "products_total must not be null");
        Assert.assertTrue(total > 0, "products_total must be > 0 for keyword: " + KW_VALID);

        List<?> products = response.jsonPath().getList("data.products");
        Assert.assertNotNull(products, "data.products must not be null");
        Assert.assertFalse(products.isEmpty(), "products list must not be empty");
        Assert.assertTrue(products.size() <= SIZE,
                "returned " + products.size() + " items but size=" + SIZE);
    }

    @Test(priority = 2, groups = {"positive"})
    @Story("TC-S02: Product fields — id/sku/name/price/image all present and valid")
    @Description("Iterates every item in data.products and asserts that the five mandatory "
            + "display fields (id, sku, name, price, image) are all non-null, "
            + "and that id is a positive integer. Catches field removal or renaming regressions.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_S02_VerifyProductFieldsAndIdPositive() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PAGE, 5, 1));
        ResponseValidator.assertStatusOk(response);

        List<Map<String, Object>> products = response.jsonPath().getList("data.products");
        Assert.assertNotNull(products, "data.products must not be null");
        Assert.assertFalse(products.isEmpty(), "data.products must not be empty");

        for (int i = 0; i < products.size(); i++) {
            Map<String, Object> p = products.get(i);
            String ctx = "products[" + i + "]";
            Assert.assertNotNull(p.get("id"),    ctx + " missing 'id'");
            Assert.assertNotNull(p.get("sku"),   ctx + " missing 'sku'");
            Assert.assertNotNull(p.get("name"),  ctx + " missing 'name'");
            Assert.assertNotNull(p.get("price"), ctx + " missing 'price'");
            Assert.assertNotNull(p.get("image"), ctx + " missing 'image'");

            int id = Integer.parseInt(p.get("id").toString());
            Assert.assertTrue(id > 0, ctx + " id must be > 0, got: " + id);

            String name = p.get("name").toString().toLowerCase();
            Assert.assertFalse(name.isEmpty(), ctx + " name must not be blank");
        }
    }

    @Test(priority = 3, groups = {"positive"})
    @Story("TC-S03: Meta fields present when has_meta_data=1")
    @Description("When the has_meta_data=1 flag is set, the response must include "
            + "data.meta_data.products_total, a non-empty sort_params list, "
            + "and the is_redirect flag. Ensures the meta block is not accidentally stripped.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_S03_MetaFieldsPresentWithFlag() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PAGE, SIZE, 1));
        ResponseValidator.assertHasakiSuccess(response);

        Assert.assertNotNull(response.jsonPath().get("data.meta_data.products_total"),
                "products_total must be present when has_meta_data=1");
        ResponseValidator.assertListNotEmpty(response, "data.meta_data.sort_params");
        Assert.assertNotNull(response.jsonPath().get("data.is_redirect"),
                "is_redirect field must be present");
    }

    @Test(priority = 4, groups = {"positive"})
    @Story("TC-S04: Pagination — page 1 and page 2 have no overlapping product IDs")
    @Description("Fetches page 1 and page 2 for the same keyword and asserts the two "
            + "product ID sets are disjoint. Catches off-by-one errors in the pagination "
            + "offset calculation on the server.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_S04_PaginationNoOverlap() {
        List<Integer> page1Ids = ApiKeyword.get(buildPath(KW_VALID, 1, SIZE, 1))
                .jsonPath().getList("data.products.id");
        List<Integer> page2Ids = ApiKeyword.get(buildPath(KW_VALID, 2, SIZE, 1))
                .jsonPath().getList("data.products.id");

        Assert.assertNotNull(page1Ids, "page 1 product ids must not be null");
        Assert.assertNotNull(page2Ids, "page 2 product ids must not be null");

        if (!page1Ids.isEmpty() && !page2Ids.isEmpty()) {
            boolean overlap = page1Ids.stream().anyMatch(page2Ids::contains);
            Assert.assertFalse(overlap, "pages 1 and 2 must not share any product ids");
        }
        LogUtils.info("TC-S04 PASS — page1=" + page1Ids.size() + ", page2=" + page2Ids.size());
    }

    @Test(priority = 5, groups = {"positive"})
    @Story("TC-S05: Custom page size respected — size=5 returns at most 5 items")
    @Description("Sends size=5 and verifies the server honours the cap. "
            + "Prevents regressions where the size param is silently ignored "
            + "and the server falls back to a default large page.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_S05_CustomPageSizeRespected() {
        int customSize = 5;
        Response response = ApiKeyword.get(buildPath(KW_VALID, PAGE, customSize, 1));
        ResponseValidator.assertStatusOk(response);

        List<?> products = response.jsonPath().getList("data.products");
        Assert.assertNotNull(products, "data.products must not be null");
        Assert.assertTrue(products.size() <= customSize,
                "returned " + products.size() + " items but size=" + customSize);
    }

    @Test(priority = 6, groups = {"positive"})
    @Story("TC-S06: Vietnamese Unicode keyword — HTTP 200, no crash")
    @Description("Sends the same concept keyword in Vietnamese Unicode ('son môi' with diacritic). "
            + "Verifies the API handles UTF-8 encoded queries without a 4xx/5xx response "
            + "and returns a parseable JSON body with products_total present.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_S06_VietnameseUnicodeKeyword() {
        Response response = ApiKeyword.get(buildPath(KW_UNICODE, PAGE, SIZE, 1));
        ResponseValidator.assertHasakiSuccess(response);
        Assert.assertNotNull(response.jsonPath().get("data.meta_data.products_total"),
                "products_total must not be null for unicode keyword");
    }

    @Test(priority = 7, groups = {"positive"})
    @Story("TC-S07: has_meta_data=0 — response still valid, no crash")
    @Description("With has_meta_data=0 the meta_data block may be absent, but the API must "
            + "still return a successful response with a non-empty product list. "
            + "Guards against the server crashing when the meta block is skipped.")
    @Severity(SeverityLevel.MINOR)
    public void TC_S07_NoMetaDataFlagStillValid() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PAGE, SIZE, 0));
        ResponseValidator.assertHasakiSuccess(response);
        ResponseValidator.assertListNotEmpty(response, "data.products");
    }

    // ── Negative ──────────────────────────────────────────────────────────────

    @Test(priority = 8, groups = {"negative", "smoke"})
    @Story("TC-S08: Non-existent keyword — error_code=0, empty result")
    @Description("A search that matches zero products should still return HTTP 200 with "
            + "error_code=0 but an empty products list and products_total=0. "
            + "Ensures no false error is raised for valid requests with no matches.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_S08_NonExistentKeywordReturnsEmpty() {
        Response response = ApiKeyword.get(buildPath(KW_NONE, PAGE, SIZE, 1));

        ResponseValidator.assertHasakiSuccess(response);

        Integer total    = response.jsonPath().get("data.meta_data.products_total");
        List<?> products = response.jsonPath().getList("data.products");
        boolean empty    = (total != null && total == 0) || (products == null || products.isEmpty());
        Assert.assertTrue(empty,
                "Non-existent keyword must return 0 products. total=" + total);
    }

    @Test(priority = 9, groups = {"negative"})
    @Story("TC-S09: Empty keyword — HTTP 200 or 400, must not 5xx")
    @Description("Submitting an empty string as the keyword is a boundary input. "
            + "The server must respond with either a client error (400) or a graceful 200 "
            + "with error_code present — never a server error (5xx).")
    @Severity(SeverityLevel.NORMAL)
    public void TC_S09_EmptyKeyword() {
        Response response = ApiKeyword.get(buildPath("", PAGE, SIZE, 1));
        int status = response.getStatusCode();

        Assert.assertTrue(status == 200 || status == 400,
                "Empty keyword must return 200 or 400, got: " + status);
        if (status == 200) {
            Assert.assertNotNull(response.jsonPath().get("status.error_code"),
                    "error_code must be present even for empty keyword");
        }
    }

    @Test(priority = 10, groups = {"negative"})
    @Story("TC-S10: Invalid pagination params — page=0, size=0 — must not 5xx")
    @Description("Tests boundary values for the pagination params: page=0 and size=0. "
            + "Neither should cause a 5xx. The server may treat them as page=1/default-size "
            + "or return a 4xx validation error — either is acceptable.")
    @Severity(SeverityLevel.MINOR)
    public void TC_S10_InvalidPaginationParams() {
        Response r1 = ApiKeyword.get(buildPath(KW_VALID, 0, SIZE, 1));
        Assert.assertTrue(r1.getStatusCode() < 500,
                "page=0 must not return 5xx, got: " + r1.getStatusCode());

        Response r2 = ApiKeyword.get(buildPath(KW_VALID, PAGE, 0, 1));
        Assert.assertTrue(r2.getStatusCode() < 500,
                "size=0 must not return 5xx, got: " + r2.getStatusCode());
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    @Test(priority = 11, groups = {"schema"})
    @Story("TC-S11: Search response schema validation")
    @Description("Validates the full search response JSON against SearchSchema.json. "
            + "Covers field names, data types, and required vs optional fields "
            + "across the entire response tree.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_S11_SearchResponseSchema() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PAGE, SIZE, 1));
        ResponseValidator.assertStatusOk(response);
        SchemaHelper.verifySchema(response, "jsonSchema/SearchSchema.json");
    }

    // ── Performance ───────────────────────────────────────────────────────────

    @Test(priority = 12, groups = {"performance"})
    @Story("TC-S12: Response time < 3000ms")
    @Description("Search is a high-frequency endpoint called on every user keystroke (debounced). "
            + "A full-page search response must complete within 3000ms to maintain UX quality.")
    @Severity(SeverityLevel.MINOR)
    public void TC_S12_SearchResponseTime() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PAGE, SIZE, 1));
        ResponseValidator.assertStatusOk(response);
        ResponseValidator.assertResponseTimeLessThan(response, 3000);
    }

    // ── Data-driven ───────────────────────────────────────────────────────────

    @Test(priority = 20,
          dataProvider = "validSearchData",
          dataProviderClass = DataProviders.class,
          groups = {"positive", "datadriven"})
    @Story("TC-S20: Data-driven — valid keywords")
    @Description("Parameterised positive test: each row in SearchData.json supplies a keyword, "
            + "pagination params, and an expectedMin product count. "
            + "Asserts error_code=0, products_total >= expectedMin, and list size <= requested size. "
            + "Covers diverse keyword types (brand names, categories, short strings) in bulk.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_S20_DataDriven_ValidKeywords(
            String tcId, String keyword, int page, int size,
            int hasMeta, int expectedMin, String description) {

        LogUtils.info("[" + tcId + "] " + description + " | keyword='" + keyword + "'");

        Response response = ApiKeyword.get(buildPath(keyword, page, size, hasMeta));
        ResponseValidator.assertHasakiSuccess(response);

        Integer total = response.jsonPath().get("data.meta_data.products_total");
        Assert.assertNotNull(total, tcId + ": products_total must not be null");
        Assert.assertTrue(total >= expectedMin,
                tcId + ": products_total=" + total + " < expectedMin=" + expectedMin);

        List<?> products = response.jsonPath().getList("data.products");
        Assert.assertNotNull(products, tcId + ": data.products must not be null");
        Assert.assertFalse(products.isEmpty(), tcId + ": products list must not be empty");
        Assert.assertTrue(products.size() <= size,
                tcId + ": returned " + products.size() + " > size=" + size);
    }

    @Test(priority = 21,
          dataProvider = "invalidSearchData",
          dataProviderClass = DataProviders.class,
          groups = {"negative", "datadriven"})
    @Story("TC-S21: Data-driven — non-existent keywords return 0 results")
    @Description("Parameterised negative test: each row in SearchInvalidData.json contains a keyword "
            + "that must produce 0 matching products (gibberish strings, impossible combinations). "
            + "Server must still return error_code=0 with an empty result set — not an error.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_S21_DataDriven_InvalidKeywords(
            String tcId, String keyword, int page, int size, int hasMeta, String description) {

        LogUtils.info("[" + tcId + "] " + description + " | keyword='" + keyword + "'");

        Response response = ApiKeyword.get(buildPath(keyword, page, size, hasMeta));
        ResponseValidator.assertHasakiSuccess(response);

        Integer total    = response.jsonPath().get("data.meta_data.products_total");
        List<?> products = response.jsonPath().getList("data.products");
        boolean empty    = (total != null && total == 0) || (products == null || products.isEmpty());
        Assert.assertTrue(empty,
                tcId + ": non-existent keyword must return 0 products. total=" + total);
    }
}
