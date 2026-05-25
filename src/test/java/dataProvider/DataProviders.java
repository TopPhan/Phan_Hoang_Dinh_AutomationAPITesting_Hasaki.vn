package dataProvider;

import com.helper.GsonDataProvider;
import org.testng.annotations.DataProvider;

/**
 * DataProviders — Tập trung toàn bộ @DataProvider cho project Hasaki.vn API Testing.
 *
 * Dữ liệu được đọc từ các file JSON trong src/test/resources/jsonData/
 * thông qua GsonDataProvider, thay vì hardcode trực tiếp trong code.
 *
 * Flow: JSON file → GsonDataProvider.loadFromResource() → Object[][] → @Test method params
 */
public class DataProviders {

    // ════════════════════════════════════════════
    // LOGIN
    // ════════════════════════════════════════════

    /**
     * Đọc từ: LoginInvalidData.json — credentials không hợp lệ (negative cases).
     * File: src/test/resources/jsonData/LoginInvalidData.json
     * Params: testCaseId, username, password, description
     */
    @DataProvider(name = "invalidCredentials")
    public Object[][] invalidCredentialsProvider() {
        return GsonDataProvider.loadFromResource(
                "jsonData/LoginInvalidData.json",
                new String[]{"testCaseId", "username", "password", "description"}
        );
    }

    // ════════════════════════════════════════════
    // SEARCH
    // ════════════════════════════════════════════

    /**
     * DataProvider cho SearchTest — keyword hợp lệ.
     * File: src/test/resources/jsonData/SearchData.json
     * Params: testCaseId, keyword, page, size, hasMetaData, expectedMinProducts, description
     */
    @DataProvider(name = "validSearchData")
    public Object[][] validSearchDataProvider() {
        return GsonDataProvider.loadFromResource(
                "jsonData/SearchData.json",
                new String[]{"testCaseId", "keyword", "page", "size",
                             "hasMetaData", "expectedMinProducts", "description"}
        );
    }

    /**
     * DataProvider cho SearchTest negative — keyword không tồn tại.
     * File: src/test/resources/jsonData/SearchInvalidData.json
     * Params: testCaseId, keyword, page, size, hasMetaData, description
     */
    @DataProvider(name = "invalidSearchData")
    public Object[][] invalidSearchDataProvider() {
        return GsonDataProvider.loadFromResource(
                "jsonData/SearchInvalidData.json",
                new String[]{"testCaseId", "keyword", "page", "size", "hasMetaData", "description"}
        );
    }

    // ════════════════════════════════════════════
    // FILTER
    // ════════════════════════════════════════════

    /**
     * DataProvider cho FilterTest — keyword + platform hợp lệ.
     * File: src/test/resources/jsonData/FilterData.json
     * Params: testCaseId, keyword, platform, description
     */
    @DataProvider(name = "validFilterData")
    public Object[][] validFilterDataProvider() {
        return GsonDataProvider.loadFromResource(
                "jsonData/FilterData.json",
                new String[]{"testCaseId", "keyword", "platform", "description"}
        );
    }

    // ════════════════════════════════════════════
    // ADD TO CART
    // ════════════════════════════════════════════

    /**
     * DataProvider cho AddToCartTest — dữ liệu hợp lệ.
     * File: src/test/resources/jsonData/AddToCartData.json
     * Params: testCaseId, productId, quantity, description
     */
    @DataProvider(name = "validAddToCartData")
    public Object[][] validAddToCartDataProvider() {
        return GsonDataProvider.loadFromResource(
                "jsonData/AddToCartData.json",
                new String[]{"testCaseId", "productId", "quantity", "description"}
        );
    }

    /**
     * DataProvider cho AddToCartTest negative — dữ liệu không hợp lệ.
     * File: src/test/resources/jsonData/AddToCartInvalidData.json
     * Params: testCaseId, productId, quantity, description
     */
    @DataProvider(name = "invalidAddToCartData")
    public Object[][] invalidAddToCartDataProvider() {
        return GsonDataProvider.loadFromResource(
                "jsonData/AddToCartInvalidData.json",
                new String[]{"testCaseId", "productId", "quantity", "description"}
        );
    }

    // ════════════════════════════════════════════
    // UPDATE CART
    // ════════════════════════════════════════════

    /**
     * DataProvider cho UpdateCartTest — dữ liệu hợp lệ.
     * File: src/test/resources/jsonData/UpdateCartData.json
     * Params: testCaseId, productId, quantity, description
     */
    @DataProvider(name = "validUpdateCartData")
    public Object[][] validUpdateCartDataProvider() {
        return GsonDataProvider.loadFromResource(
                "jsonData/UpdateCartData.json",
                new String[]{"testCaseId", "productId", "quantity", "description"}
        );
    }

    /**
     * DataProvider cho UpdateCartTest negative — dữ liệu không hợp lệ.
     * File: src/test/resources/jsonData/UpdateCartInvalidData.json
     * Params: testCaseId, productId, quantity, description
     */
    @DataProvider(name = "invalidUpdateCartData")
    public Object[][] invalidUpdateCartDataProvider() {
        return GsonDataProvider.loadFromResource(
                "jsonData/UpdateCartInvalidData.json",
                new String[]{"testCaseId", "productId", "quantity", "description"}
        );
    }

    // ════════════════════════════════════════════
    // DELETE CART
    // ════════════════════════════════════════════

    /**
     * DataProvider cho DeleteItemInCartTest — dữ liệu hợp lệ.
     * File: src/test/resources/jsonData/DeleteCartData.json
     * Params: testCaseId, productId, quantity, description
     */
    @DataProvider(name = "validDeleteCartData")
    public Object[][] validDeleteCartDataProvider() {
        return GsonDataProvider.loadFromResource(
                "jsonData/DeleteCartData.json",
                new String[]{"testCaseId", "productId", "quantity", "description"}
        );
    }

    /**
     * DataProvider cho DeleteItemInCartTest negative — dữ liệu không hợp lệ.
     * File: src/test/resources/jsonData/DeleteCartInvalidData.json
     * Params: testCaseId, productId, quantity, description
     */
    @DataProvider(name = "invalidDeleteCartData")
    public Object[][] invalidDeleteCartDataProvider() {
        return GsonDataProvider.loadFromResource(
                "jsonData/DeleteCartInvalidData.json",
                new String[]{"testCaseId", "productId", "quantity", "description"}
        );
    }
}
