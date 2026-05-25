package com.listener;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * RetryListener — tự động gắn RetryAnalyzer cho TOÀN BỘ @Test trong suite.
 *
 * Vì sao cần class này?
 *   Nếu chỉ có RetryAnalyzer, bạn phải gắn retryAnalyzer = RetryAnalyzer.class
 *   trên từng @Test method → rất tốn công khi có hàng trăm test cases.
 *   RetryListener dùng IAnnotationTransformer để inject RetryAnalyzer tự động
 *   vào tất cả @Test lúc TestNG khởi động, không cần sửa test class.
 *
 * Cách dùng — thêm vào suite XML (PHẢI thêm CẢ HAI listener):
 *
 *   <listeners>
 *       <listener class-name="com.listener.TestListener"/>
 *       <listener class-name="com.listener.RetryListener"/>
 *   </listeners>
 *
 * Lưu ý: RetryListener phải implement IAnnotationTransformer,
 * KHÔNG phải ITestListener — đây là annotation-time hook, không phải runtime hook.
 */
public class RetryListener implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {
        // Chỉ set RetryAnalyzer nếu @Test chưa có retryAnalyzer riêng
        // → Tôn trọng override ở từng test nếu cần tuỳ chỉnh
        if (annotation.getRetryAnalyzerClass() == null) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
