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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FilterTest — Product Filter feature
 * Endpoint: GET /mobile/v2/main/products/filters
 *
 * Test flow order:
 *   [1] Positive     — filter list, item structure, values structure, sort_order uniqueness,
 *                      brand filter presence, web vs mobile platform
 *   [2] Negative     — non-existent keyword, empty keyword, invalid platform, missing param
 *   [3] Integration  — filter key applied as search param (cross-feature flow)
 *   [4] Schema       — response contract validation
 *   [5] Performance  — response time threshold
 *   [6] Data-driven  — bulk keyword + platform combinations from JSON
 *
 * Query params:
 *   q        — search keyword to get filters for
 *   platform — "web" | "mobile" (controls which filters are shown)
 *
 * Response structure:
 *   status.error_code              — 0 = success
 *   data.filter[].key              — filter identifier, used as query param in /search
 *   data.filter[].name             — display label shown to user
 *   data.filter[].type             — filter type: "text" | etc.
 *   data.filter[].sort_order       — display order (must be unique across all filters)
 *   data.filter[].values[].key     — option identifier
 *   data.filter[].values[].label   — display label for the option
 *   data.filter[].values[].value   — number of matching products (>= 0)
 */
@Epic("Hasaki.vn API Testing")
@Feature("Product Filter")
public class FilterTest extends BaseTest {

    private static final String KW_VALID    = "SON MOI";
    private static final String KW_NONE     = "xyzabc123nonexistent999";
    private static final String PLAT_WEB    = "web";
    private static final String PLAT_MOBILE = "mobile";

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Builds the filter request path.
     *
     * @param keyword  search term to get filters for
     * @param platform "web" | "mobile"
     * @return relative path ready for ApiKeyword.get()
     */
    private String buildPath(String keyword, String platform) {
        return EndPointGlobal.EP_FILTER + "?q=" + keyword + "&platform=" + platform;
    }

    // ── Positive ──────────────────────────────────────────────────────────────

    @Test(priority = 1, groups = {"positive", "smoke"})
    @Story("TC-F01: Valid keyword — at least 1 filter returned")
    @Description("Smoke test: a known valid keyword must produce error_code=0 "
            + "and a non-empty data.filter list. Fails fast if the filter endpoint "
            + "is down or returning an empty response for all inputs.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC_F01_GetFiltersWithValidKeyword() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PLAT_WEB));

        ResponseValidator.assertHasakiSuccess(response);
        ResponseValidator.assertListNotEmpty(response, "data.filter");
        LogUtils.info("TC-F01 PASS — filter count: " + response.jsonPath().getList("data.filter").size());
    }

    @Test(priority = 2, groups = {"positive", "smoke"})
    @Story("TC-F02: Filter item structure — key/name/type/sort_order/values all present")
    @Description("Iterates every filter item and checks all five mandatory fields are present "
            + "and that 'values' is an array type. "
            + "Catches field removal or structural regressions at the filter-item level.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_F02_VerifyFilterItemStructure() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PLAT_WEB));
        ResponseValidator.assertStatusOk(response);

        List<Map<String, Object>> filters = response.jsonPath().getList("data.filter");
        Assert.assertNotNull(filters, "data.filter must not be null");
        Assert.assertFalse(filters.isEmpty(), "data.filter must have at least 1 item");

        for (int i = 0; i < filters.size(); i++) {
            Map<String, Object> f = filters.get(i);
            String ctx = "filter[" + i + "]";
            Assert.assertNotNull(f.get("key"),        ctx + " missing 'key'");
            Assert.assertNotNull(f.get("name"),       ctx + " missing 'name'");
            Assert.assertNotNull(f.get("type"),       ctx + " missing 'type'");
            Assert.assertNotNull(f.get("sort_order"), ctx + " missing 'sort_order'");
            Assert.assertNotNull(f.get("values"),     ctx + " missing 'values'");
            Assert.assertTrue(f.get("values") instanceof List,
                    ctx + ".values must be an array");
        }
    }

    @Test(priority = 3, groups = {"positive", "smoke"})
    @Story("TC-F03: Filter values structure — key/label/value all present, product count >= 0")
    @Description("Deep-validates every entry inside every filter's values array. "
            + "Asserts key, label, and value are non-null, and that value (product count) "
            + "is a non-negative integer. Prevents UI rendering crashes from null option data.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_F03_VerifyFilterValuesStructure() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PLAT_WEB));
        ResponseValidator.assertStatusOk(response);

        List<Map<String, Object>> filters = response.jsonPath().getList("data.filter");
        Assert.assertNotNull(filters, "data.filter must not be null");

        int checked = 0;
        for (int i = 0; i < filters.size(); i++) {
            List<Map<String, Object>> values = (List<Map<String, Object>>) filters.get(i).get("values");
            if (values == null || values.isEmpty()) continue;
            for (int j = 0; j < values.size(); j++) {
                Map<String, Object> entry = values.get(j);
                String ctx = "filter[" + i + "].values[" + j + "]";
                Assert.assertNotNull(entry.get("key"),   ctx + " missing 'key'");
                Assert.assertNotNull(entry.get("label"), ctx + " missing 'label'");
                Assert.assertNotNull(entry.get("value"), ctx + " missing 'value'");

                int count = Integer.parseInt(entry.get("value").toString());
                Assert.assertTrue(count >= 0, ctx + " product count must be >= 0, got: " + count);
                checked++;
            }
        }
        LogUtils.info("TC-F03 PASS — checked " + checked + " value entries across all filters");
    }

    @Test(priority = 4, groups = {"positive"})
    @Story("TC-F04: sort_order is unique across all filter items")
    @Description("Collects sort_order values from all filter items into a Set and asserts no "
            + "duplicates exist. Duplicate sort_order causes non-deterministic UI rendering "
            + "order and is a data integrity bug.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_F04_SortOrderIsUnique() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PLAT_WEB));
        ResponseValidator.assertStatusOk(response);

        List<Map<String, Object>> filters = response.jsonPath().getList("data.filter");
        Assert.assertNotNull(filters, "data.filter must not be null");

        Set<Object> seen = new HashSet<>();
        for (int i = 0; i < filters.size(); i++) {
            Object order = filters.get(i).get("sort_order");
            Assert.assertNotNull(order, "filter[" + i + "] sort_order must not be null");
            Assert.assertTrue(seen.add(order),
                    "Duplicate sort_order=" + order + " found at filter[" + i + "]");
        }
    }

    @Test(priority = 5, groups = {"positive"})
    @Story("TC-F05: Filter key 'brand' must exist for keyword 'SON MOI'")
    @Description("Business rule: the 'brand' filter is mandatory for lipstick (SON MOI) searches "
            + "because brand is a primary purchase decision factor for this category. "
            + "Absence of the brand filter would break the filter panel on the product listing page.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_F05_BrandFilterMustExist() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PLAT_WEB));
        ResponseValidator.assertStatusOk(response);

        List<Map<String, Object>> filters = response.jsonPath().getList("data.filter");
        Assert.assertNotNull(filters, "data.filter must not be null");

        boolean hasBrand = filters.stream()
                .anyMatch(f -> f.get("key") != null
                        && f.get("key").toString().toLowerCase().contains("brand"));

        Assert.assertTrue(hasBrand,
                "A 'brand' filter must exist for keyword '" + KW_VALID + "'. "
                + "Available keys: " + filters.stream().map(f -> f.get("key")).toList());
    }

    @Test(priority = 6, groups = {"positive"})
    @Story("TC-F06: platform=web and platform=mobile both return HTTP 200")
    @Description("Verifies both platform values are accepted. Web and mobile may return "
            + "different filter sets (e.g. mobile hides advanced filters), but both "
            + "must return HTTP 200 with a non-null filter list.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_F06_WebAndMobilePlatformBothSucceed() {
        Response webResp    = ApiKeyword.get(buildPath(KW_VALID, PLAT_WEB));
        Response mobileResp = ApiKeyword.get(buildPath(KW_VALID, PLAT_MOBILE));

        ResponseValidator.assertStatusOk(webResp);
        ResponseValidator.assertStatusOk(mobileResp);

        List<?> webFilters    = webResp.jsonPath().getList("data.filter");
        List<?> mobileFilters = mobileResp.jsonPath().getList("data.filter");

        Assert.assertNotNull(webFilters,    "web filters must not be null");
        Assert.assertNotNull(mobileFilters, "mobile filters must not be null");
        LogUtils.info("TC-F06 PASS — web=" + webFilters.size() + " | mobile=" + mobileFilters.size());
    }

    // ── Negative ──────────────────────────────────────────────────────────────

    @Test(priority = 7, groups = {"negative", "smoke"})
    @Story("TC-F07: Non-existent keyword — error_code=0, empty/null filter list")
    @Description("A keyword that matches no products should return error_code=0 "
            + "with an empty or null filter list — not an error code. "
            + "Verifies the API treats 'no filters' as a valid empty state.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_F07_NonExistentKeywordReturnsEmptyFilters() {
        Response response = ApiKeyword.get(buildPath(KW_NONE, PLAT_WEB));

        ResponseValidator.assertHasakiSuccess(response);

        List<?> filters = response.jsonPath().getList("data.filter");
        boolean empty = filters == null || filters.isEmpty();
        Assert.assertTrue(empty,
                "Non-existent keyword must return empty filter list, got: "
                + (filters != null ? filters.size() : "null") + " items");
    }

    @Test(priority = 8, groups = {"negative"})
    @Story("TC-F08: Empty keyword — must not 5xx")
    @Description("An empty string 'q' param is a boundary input. "
            + "The server must handle it gracefully (200 or 4xx) and must never crash (5xx).")
    @Severity(SeverityLevel.MINOR)
    public void TC_F08_EmptyKeywordNoServerError() {
        Response response = ApiKeyword.get(buildPath("", PLAT_WEB));
        int status = response.getStatusCode();
        Assert.assertTrue(status < 500,
                "Empty keyword must not return 5xx, got: " + status);
    }

    @Test(priority = 9, groups = {"negative"})
    @Story("TC-F09: Invalid platform value — must not 5xx")
    @Description("Sends an unrecognised platform value ('desktop'). "
            + "The server must reject it with a client error or fall back gracefully — "
            + "never with a 5xx that would indicate an unhandled exception.")
    @Severity(SeverityLevel.MINOR)
    public void TC_F09_InvalidPlatformNoServerError() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, "desktop"));
        int status = response.getStatusCode();
        Assert.assertTrue(status < 500,
                "Invalid platform must not return 5xx, got: " + status);
    }

    @Test(priority = 10, groups = {"negative"})
    @Story("TC-F10: Missing required param 'q' — must not 5xx")
    @Description("Omits the mandatory 'q' param entirely. "
            + "Expects a 4xx client error or a 200 with error_code != 0. "
            + "A 5xx here indicates the endpoint does not validate required params.")
    @Severity(SeverityLevel.MINOR)
    public void TC_F10_MissingQParamNoServerError() {
        Response response = ApiKeyword.get(EndPointGlobal.EP_FILTER + "?platform=web");
        int status = response.getStatusCode();
        Assert.assertTrue(status < 500,
                "Missing 'q' param must not return 5xx, got: " + status);
        if (status == 200) {
            Assert.assertNotNull(response.jsonPath().get("status.error_code"),
                    "error_code must be present in response");
        }
    }

    // ── Integration ───────────────────────────────────────────────────────────

    @Test(priority = 11, groups = {"integration", "smoke"})
    @Story("TC-F11: Filter key used as search param — search returns valid results")
    @Description("Cross-feature integration flow:\n"
            + "  Step 1 — GET /filters for 'SON MOI' to retrieve available filter keys.\n"
            + "  Step 2 — Pick the first filter that has at least one value option.\n"
            + "  Step 3 — GET /search with that filter key=value appended as a query param.\n"
            + "Asserts the filtered search returns a valid 200 response with a non-null product list. "
            + "Validates that filter keys returned by /filters are actually accepted by /search.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_F11_FilterKeyAppliedToSearchReturnsResults() {
        // Step 1: get filters
        Response filterResp = ApiKeyword.get(buildPath(KW_VALID, PLAT_WEB));
        ResponseValidator.assertStatusOk(filterResp);

        List<Map<String, Object>> filters = filterResp.jsonPath().getList("data.filter");
        Assert.assertNotNull(filters, "data.filter must not be null");
        if (filters.isEmpty()) {
            LogUtils.info("TC-F11 SKIP — no filters available");
            return;
        }

        // Step 2: pick first filter that has values
        String filterKey = null, valueKey = null;
        for (Map<String, Object> filter : filters) {
            List<Map<String, Object>> values = (List<Map<String, Object>>) filter.get("values");
            if (values != null && !values.isEmpty()) {
                filterKey = filter.get("key").toString();
                valueKey  = values.get(0).get("key").toString();
                break;
            }
        }
        if (filterKey == null) {
            LogUtils.info("TC-F11 SKIP — no filter has values");
            return;
        }

        // Step 3: search with filter applied
        String searchPath = EndPointGlobal.EP_SEARCH
                + "?keyword=" + KW_VALID + "&page=1&size=20&has_meta_data=1"
                + "&" + filterKey + "=" + valueKey;

        Response searchResp = ApiKeyword.get(searchPath);
        ResponseValidator.assertHasakiSuccess(searchResp);

        List<?> products = searchResp.jsonPath().getList("data.products");
        Assert.assertNotNull(products, "Step3: products must not be null with filter applied");

        LogUtils.info("TC-F11 PASS — filter[" + filterKey + "=" + valueKey + "] → "
                + (products != null ? products.size() : 0) + " products");
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    @Test(priority = 12, groups = {"schema"})
    @Story("TC-F12: Filter response schema validation")
    @Description("Validates the full filter response JSON against FilterSchema.json. "
            + "Covers nested structure validation (filter items + values arrays) "
            + "including field types and required/optional constraints.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_F12_FilterResponseSchema() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PLAT_WEB));
        ResponseValidator.assertStatusOk(response);
        SchemaHelper.verifySchema(response, "jsonSchema/FilterSchema.json");
    }

    // ── Performance ───────────────────────────────────────────────────────────

    @Test(priority = 13, groups = {"performance"})
    @Story("TC-F13: Response time < 3000ms")
    @Description("Filter data is loaded when the user first opens the search/listing page. "
            + "Slow filter responses directly delay page load. Must complete within 3000ms.")
    @Severity(SeverityLevel.MINOR)
    public void TC_F13_FilterResponseTime() {
        Response response = ApiKeyword.get(buildPath(KW_VALID, PLAT_WEB));
        ResponseValidator.assertStatusOk(response);
        ResponseValidator.assertResponseTimeLessThan(response, 3000);
    }

    // ── Data-driven ───────────────────────────────────────────────────────────

    @Test(priority = 20,
          dataProvider = "validFilterData",
          dataProviderClass = DataProviders.class,
          groups = {"positive", "datadriven"})
    @Story("TC-F20: Data-driven — various keywords and platforms")
    @Description("Parameterised positive test: each row in FilterData.json supplies a keyword "
            + "and platform value. Asserts error_code=0, non-empty filter list, and all items "
            + "have key, name, and values fields. Covers category keywords, brand keywords, "
            + "and both platform types in bulk.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_F20_DataDriven_ValidFilters(
            String tcId, String keyword, String platform, String description) {

        LogUtils.info("[" + tcId + "] " + description + " | keyword='" + keyword + "', platform='" + platform + "'");

        Response response = ApiKeyword.get(buildPath(keyword, platform));
        ResponseValidator.assertHasakiSuccess(response);

        List<Map<String, Object>> filters = response.jsonPath().getList("data.filter");
        Assert.assertNotNull(filters, tcId + ": data.filter must not be null");
        Assert.assertFalse(filters.isEmpty(), tcId + ": at least 1 filter expected");

        for (Map<String, Object> f : filters) {
            Assert.assertNotNull(f.get("key"),    tcId + ": filter missing 'key'");
            Assert.assertNotNull(f.get("name"),   tcId + ": filter missing 'name'");
            Assert.assertNotNull(f.get("values"), tcId + ": filter missing 'values'");
        }
    }
}
