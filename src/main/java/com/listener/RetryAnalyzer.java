package com.listener;

import com.helper.LogUtils;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * RetryAnalyzer — tự động retry test bị fail do lỗi mạng / flaky session.
 *
 * Cách hoạt động:
 *   - Mỗi test method có tối đa MAX_RETRY lần retry (không tính lần chạy đầu).
 *   - Chỉ retry khi lỗi là "có thể do môi trường" (network, timeout, 5xx).
 *   - Lỗi do assertion logic (assert sai giá trị) → KHÔNG retry → fail ngay.
 *
 * Cách dùng — 2 cách:
 *
 *   Cách 1: Gắn trực tiếp trên @Test
 *   ──────────────────────────────────
 *   @Test(retryAnalyzer = RetryAnalyzer.class)
 *   public void TC_C01_AddToCart() { ... }
 *
 *   Cách 2: Dùng RetryListener để tự động apply cho toàn bộ suite (khuyên dùng)
 *   ────────────────────────────────────────────────────────────────────────────
 *   Thêm RetryListener vào suite XML:
 *   <listeners>
 *       <listener class-name="com.listener.RetryListener"/>
 *   </listeners>
 *
 *   → Khi đó KHÔNG cần gắn retryAnalyzer = RetryAnalyzer.class trên từng @Test.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    // Số lần retry tối đa (không tính lần chạy đầu tiên)
    private static final int MAX_RETRY = 2;

    // Đếm số lần đã retry — mỗi instance gắn với 1 test method
    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount >= MAX_RETRY) {
            return false; // Đã hết lượt retry
        }

        Throwable cause = result.getThrowable();

        // Không retry lỗi assertion thuần (logic sai) — chỉ retry lỗi môi trường
        if (!isRetryable(cause)) {
            LogUtils.warn("[RetryAnalyzer] Non-retryable failure — skip retry: "
                    + result.getName()
                    + " | Cause: " + (cause != null ? cause.getMessage() : "unknown"));
            return false;
        }

        retryCount++;
        LogUtils.warn("[RetryAnalyzer] Retrying test ["
                + result.getName() + "] — attempt " + retryCount + "/" + MAX_RETRY
                + " | Cause: " + (cause != null ? cause.getClass().getSimpleName() : "unknown"));
        return true;
    }

    /**
     * Xác định lỗi có thể retry hay không.
     *
     * Retry khi:
     *   - Lỗi kết nối / timeout (SocketException, ConnectException, SocketTimeoutException)
     *   - Server trả 5xx (RuntimeException từ RestAssured chứa "500"/"502"/"503"/"504")
     *   - Lỗi đọc response (JsonParseException, IllegalStateException từ body rỗng)
     *
     * KHÔNG retry khi:
     *   - AssertionError (assert logic trong test sai — đây là bug thật)
     *   - NullPointerException từ code test (bug trong test script)
     */
    private boolean isRetryable(Throwable cause) {
        if (cause == null) return false;

        // AssertionError = test logic fail → không retry
        if (cause instanceof AssertionError) return false;

        String message = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
        String className = cause.getClass().getName().toLowerCase();

        // Network / connection errors
        if (className.contains("socket")
                || className.contains("connect")
                || className.contains("timeout")
                || className.contains("ioexception")) {
            return true;
        }

        // Server-side errors (5xx) — RestAssured ném RuntimeException với status code trong message
        if (message.contains("500")
                || message.contains("502")
                || message.contains("503")
                || message.contains("504")
                || message.contains("timed out")
                || message.contains("connection refused")) {
            return true;
        }

        // Response parse errors (body rỗng, JSON malformed)
        if (className.contains("jsonparse")
                || className.contains("illegalstate")
                || message.contains("expected begin_object")
                || message.contains("end of input")) {
            return true;
        }

        return false;
    }
}
