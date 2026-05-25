package com.listener;

import com.globals.ConfigsGlobal;
import com.helper.LogUtils;
import com.reports.AllureManager;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import com.listener.RetryAnalyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * TestListener — cập nhật so với phiên bản cũ:
 *   - Bỏ PropertiesHelper.loadAllFiles() trong onStart (không còn cần thiết, EnvConfig tự load)
 *   - Thêm ENV_NAME vào Allure environment.properties để report hiển thị đang chạy env nào
 *   - Thêm TIMEOUT info vào report
 */
public class TestListener implements ITestListener {

    @Override
    public void onStart(ITestContext context) {
        // EnvConfig đã tự load khi class được khởi tạo lần đầu
        // Không cần gọi PropertiesHelper.loadAllFiles() nữa
        LogUtils.info("\n===============================================================\n"
                + "  " + context.getSuite().getName().toUpperCase() + " — STARTING"
                + "\n  Environment : " + ConfigsGlobal.ACTIVE_ENV.toUpperCase()
                + "\n  Base URI    : " + ConfigsGlobal.BASE_URI
                + "\n===============================================================");
    }

    @Override
    public void onFinish(ITestContext context) {
        LogUtils.info("\n============================= FINISHED ==============================");
        LogUtils.info("Total:  " + ConfigsGlobal.TCS_TOTAL);
        LogUtils.info("Passed: " + ConfigsGlobal.PASSED_TOTAL);
        LogUtils.info("Failed: " + ConfigsGlobal.FAILED_TOTAL);
        writeAllureEnvironment();
    }

    @Override
    public void onTestStart(ITestResult result) {
        LogUtils.info("\n>>> Running: " + result.getName());
        ConfigsGlobal.TCS_TOTAL++;
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LogUtils.info("PASS: " + result.getName()
                + " (" + result.getEndMillis() + result.getStartMillis() + " ms)");
        AllureManager.saveTextLog("PASS: " + result.getName());
        ConfigsGlobal.PASSED_TOTAL++;
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LogUtils.error("FAIL: " + result.getName());
        LogUtils.error(result.getThrowable());
        AllureManager.saveTextLog("FAIL: " + result.getName());
        ConfigsGlobal.FAILED_TOTAL++;
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LogUtils.warn("SKIP: " + result.getName());
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void writeAllureEnvironment() {
        try {
            Properties props = new Properties();
            props.setProperty("Environment",  ConfigsGlobal.ACTIVE_ENV.toUpperCase());
            props.setProperty("Base URL",     ConfigsGlobal.BASE_URI != null ? ConfigsGlobal.BASE_URI : "");
            props.setProperty("Framework",    "RestAssured + TestNG");
            props.setProperty("Java",         System.getProperty("java.version"));
            props.setProperty("OS",           System.getProperty("os.name"));
            props.setProperty("Project",      "Hasaki.vn API Testing");

            File dir = new File("allure-results/");
            if (!dir.exists()) dir.mkdirs();

            try (FileOutputStream fos = new FileOutputStream(new File(dir, "environment.properties"))) {
                props.store(fos, null);
            }
        } catch (Exception e) {
            LogUtils.error("[TestListener] Failed to write Allure environment file: " + e.getMessage());
        }
    }
}
