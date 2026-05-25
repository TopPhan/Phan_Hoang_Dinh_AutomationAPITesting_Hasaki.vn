![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![REST Assured](https://img.shields.io/badge/REST--Assured-5.5.6-brightgreen?style=flat-square)
![TestNG](https://img.shields.io/badge/TestNG-7.12.0-red?style=flat-square)
![Maven](https://img.shields.io/badge/Maven-Project-blue?style=flat-square&logo=apache-maven) <br>
![API Testing](https://github.com/TopPhan/Phan_Hoang_Dinh_AutomationTesting_Hasaki.vn/actions/workflows/API_Testing.yml/badge.svg)

# 🌿 [Hasaki.vn](https://hasaki.vn/) — API Testing Framework

<!-- PROJECT SCREENSHOT PLACEHOLDER -->
<!-- <img width="1911" height="866" alt="image" src="YOUR_SCREENSHOT_URL" /> -->

---

[![Allure Report](https://img.shields.io/badge/Allure%20Report-View%20Here-ff69b4?style=for-the-badge&logo=allure)](https://TopPhan.github.io/Phan_Hoang_Dinh_API_Testing_Hasaki.vn/)

<!-- ALLURE REPORT SCREENSHOTS PLACEHOLDER -->
<!-- <img width="1919" height="872" alt="image" src="YOUR_ALLURE_SCREENSHOT_URL" /> -->

---

## 📋 Table of Contents

- [Project Overview](#-project-overview)
- [Tech Stack](#%EF%B8%8F-technology-stack)
- [Project Structure](#-project-structure)
- [Architecture & Flow Diagrams](#-architecture--flow-diagrams)
- [Key Technologies Explained](#-key-technologies-explained)
- [Test Suites & Coverage](#-test-suites--coverage)
- [Data-Driven Testing](#-data-driven-testing)
- [Multi-Environment Support](#-multi-environment-support)
- [Security — Credential Management](#-security--credential-management)
- [Configuration & How to Run](#%EF%B8%8F-configuration--how-to-run-the-project)
- [Allure Report](#-allure-report)
- [CI/CD Pipeline](#%EF%B8%8F-cicd-pipeline--github-actions)

---

## 🌿 Project Overview

**Hasaki.vn API Testing** là framework kiểm thử API production-grade nhắm vào hệ thống backend của [Hasaki.vn](https://hasaki.vn/) — nền tảng thương mại điện tử mỹ phẩm & chăm sóc da hàng đầu Việt Nam.  
Xây dựng với **REST Assured**, **TestNG**, và kiến trúc **Keyword-Driven + Data-Driven**, framework kiểm thử toàn bộ luồng API từ Authentication, Search, Filter đến Cart management — với Allure Report tự động deploy lên GitHub Pages sau mỗi CI run.

| Metric | Result |
|---|---|
| 🧪 Tổng số Test Cases | **82 tests** (Positive · Negative · Security · Schema · Performance) |
| 🔗 Endpoints được bao phủ | **6 endpoints** (Login · Search · Filter · Add/Update/Delete Cart) |
| 🌐 Multi-Environment | **Dev · Staging · Production** — chuyển môi trường bằng `-Denv` |
| 📈 Allure Report | Auto-deploy lên **GitHub Pages** — giữ lại 20 lần chạy gần nhất |
| 🤖 CI Platform | **GitHub Actions (Ubuntu Latest)** |
| 🔐 Credential Security | **GitHub Secrets** — không hardcode credentials trong source code |
| 🔄 Retry Mechanism | **RetryAnalyzer** — tự động retry test flaky tối đa 2 lần |

### 🌟 Key Strengths

- **Keyword-Driven Design** — `ApiKeyword` bọc REST Assured thành các method ngắn gọn (`get`, `post`, `put`, `delete`), test case đọc như tài liệu nghiệp vụ, không lộ HTTP plumbing.
- **Centralized Validation** — `ResponseValidator` tập trung toàn bộ assertion logic. Khi response contract thay đổi, chỉ cần sửa 1 chỗ thay vì cập nhật từng test file.
- **Multi-Environment** — `EnvConfig` load đúng file config theo `-Denv` (dev / staging / prod). Chuyển môi trường chỉ cần 1 flag, không sửa code.
- **Data-Driven Testing** — Test data externalize ra JSON theo từng feature (valid / invalid data), kết hợp `GsonDataProvider` và `@DataProvider` của TestNG cho phép mở rộng test case mà không thay đổi logic.
- **JSON Schema Validation** — Mỗi endpoint có file `.json` Schema riêng, đảm bảo response contract không bị phá vỡ giữa các phiên bản API.
- **Live Allure Reporting** — Mỗi CI run tự công bố báo cáo tương tác lên GitHub Pages với lịch sử 20 lần chạy.
- **Secure Credential Management** — Credentials được inject vào file `.properties` đúng môi trường tại runtime bằng Python; không có thông tin nhạy cảm nào được commit vào source.
- **Auto Retry on Flaky Tests** — `RetryAnalyzer` + `RetryListener` tự động retry test thất bại tối đa 2 lần trước khi báo cáo fail thực sự.

---

## 🧱 Project Structure

```
Hasaki.vn_API_Testing/
├── .github/
│   └── workflows/
│       └── API_Testing.yml              # GitHub Actions CI pipeline
│
├── src/
│   ├── main/
│   │   ├── java/com/
│   │   │   ├── globals/
│   │   │   │   ├── ConfigsGlobal.java   # Central config holder (BASE_URI, credentials...)
│   │   │   │   ├── EndPointGlobal.java  # All API endpoint constants
│   │   │   │   ├── EnvConfig.java       # Multi-env loader (-Denv=dev/staging/prod)
│   │   │   │   └── TokenGlobal.java     # Global token storage (verify_token, cookies)
│   │   │   ├── helper/
│   │   │   │   ├── GsonDataProvider.java  # JSON → TestNG @DataProvider bridge
│   │   │   │   ├── JsonHelper.java        # JSON read/write utilities
│   │   │   │   ├── LogUtils.java          # Log4j2 wrapper
│   │   │   │   ├── PropertiesHelper.java  # .properties file reader
│   │   │   │   ├── SchemaHelper.java      # JSON Schema loader helper
│   │   │   │   └── SystemHelper.java      # System/OS utilities
│   │   │   ├── keywords/
│   │   │   │   ├── ApiKeyword.java        # Core HTTP methods (get, post, put, delete)
│   │   │   │   ├── GetSection.java        # Reusable GET response extraction
│   │   │   │   └── SpecBuilder.java       # REST Assured RequestSpec / ResponseSpec builder
│   │   │   ├── listener/
│   │   │   │   ├── RetryAnalyzer.java     # Flaky test retry logic (max 2 retries)
│   │   │   │   ├── RetryListener.java     # Wires RetryAnalyzer to all tests
│   │   │   │   └── TestListener.java      # Allure environment, log, summary on finish
│   │   │   ├── reports/
│   │   │   │   └── AllureManager.java     # Allure attachment helpers
│   │   │   └── validator/
│   │   │       └── ResponseValidator.java # Centralized assertion engine
│   │   └── resources/
│   │       ├── log4j2.properties
│   │       └── TutorialUse/               # Developer reference docs for helpers
│   │
│   └── test/
│       └── java/com/
│           ├── baseSetup/
│           │   └── BaseTest.java          # Suite-level setup: login once, share token
│           ├── builder/
│           │   └── LoginPOJO_Builder.java # Builder pattern for login request body
│           ├── pojoModel/
│           │   ├── AddToCartModel.java
│           │   ├── DeleteCartModel.java
│           │   ├── LoginModel.java
│           │   └── UpdateCartModel.java
│           ├── testScenarios/
│           │   ├── LoginTest.java         # 8 tests — Authentication
│           │   ├── SearchTest.java        # 14 tests — Search API
│           │   ├── FilterTest.java        # 14 tests — Filter/Browse API
│           │   ├── AddToCartTest.java     # 13 tests — Add to Cart
│           │   ├── UpdateCartTest.java    # 17 tests — Update Cart quantity
│           │   └── DeleteItemInCartTest.java # 16 tests — Delete Cart item
│           └── dataProvider/
│               └── DataProviders.java     # All @DataProvider methods
│
├── src/test/resources/
│   ├── configs/
│   │   ├── dev.properties               # Local dev environment
│   │   ├── staging.properties           # Staging / UAT environment
│   │   └── prod.properties              # Production environment
│   ├── jsonData/
│   │   ├── Login.json                   # Valid login credentials
│   │   ├── LoginInvalidData.json        # Negative login scenarios
│   │   ├── SearchData.json              # Valid search keywords
│   │   ├── SearchInvalidData.json       # Edge-case search keywords
│   │   ├── FilterData.json              # Valid filter params
│   │   ├── AddToCartData.json           # Valid add-to-cart payloads
│   │   ├── AddToCartInvalidData.json    # Invalid add-to-cart payloads
│   │   ├── UpdateCartData.json          # Valid update-cart payloads
│   │   ├── UpdateCartInvalidData.json   # Invalid update-cart payloads
│   │   ├── DeleteCartData.json          # Valid delete-cart payloads
│   │   └── DeleteCartInvalidData.json   # Invalid delete-cart payloads
│   ├── jsonSchema/
│   │   ├── LoginSchema.json             # Login response contract
│   │   ├── SearchSchema.json            # Search response contract
│   │   ├── FilterSchema.json            # Filter response contract
│   │   └── AddToCartSchema.json         # Add-to-cart response contract
│   └── suites/
│       ├── SuiteSmoke.xml               # ~2–3 min — BLOCKER/CRITICAL checks
│       ├── SuiteSanity.xml              # Core positive scenarios
│       ├── SuiteRegression.xml          # Full regression
│       ├── SuiteFull.xml                # All test groups
│       ├── SuiteNegative.xml            # Negative & edge cases only
│       ├── SuitePerformance.xml         # Response time thresholds
│       ├── SuiteSecurity.xml            # CSRF, auth bypass, brute-force
│       ├── SuiteDataDriven.xml          # Data-driven scenarios
│       └── SuiteEndToEnd.xml            # Full user journey (Login → Cart)
│
├── allure-results/
├── logs/
├── pom.xml
└── .gitignore
```

---

## 🔄 Architecture & Flow Diagrams

### Keyword-Driven API Testing Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      TEST SCENARIO LAYER                        │
│  LoginTest │ SearchTest │ FilterTest │ AddToCartTest │ ...       │
└────────────────────────┬────────────────────────────────────────┘
                         │ calls
┌────────────────────────▼────────────────────────────────────────┐
│                       KEYWORD LAYER                             │
│         ApiKeyword.get() │ .post() │ .put() │ .delete()         │
└────────────────────────┬────────────────────────────────────────┘
                         │ validates with
┌────────────────────────▼────────────────────────────────────────┐
│                     VALIDATION LAYER                            │
│   ResponseValidator.assertHasakiSuccess() │ assertErrorCode()   │
│   assertFieldEquals() │ assertSchema() │ assertResponseTime()    │
└────────────────────────┬────────────────────────────────────────┘
                         │ built by
┌────────────────────────▼────────────────────────────────────────┐
│                    SPEC / CONFIG LAYER                          │
│   SpecBuilder (RequestSpec + ResponseSpec) │ ConfigsGlobal      │
│   EnvConfig (dev / staging / prod)        │ TokenGlobal         │
└────────────────────────┬────────────────────────────────────────┘
                         │ sends HTTP to
┌────────────────────────▼────────────────────────────────────────┐
│                    REST ASSURED ENGINE                          │
│              Hasaki.vn Mobile API Backend                       │
└─────────────────────────────────────────────────────────────────┘
```

### CI/CD Pipeline Flow

```
  Push to master / PR / Manual Dispatch (chọn suite + env)
              │
              ▼
      ┌───────────────────┐
      │  Checkout Code    │  (~20s)
      └────────┬──────────┘
               │
      ┌────────▼──────────┐
      │  Set up JDK 21    │  (~2s)
      └────────┬──────────┘
               │
      ┌────────▼──────────────────────────────┐
      │  Inject Credentials into env config   │
      │  GitHub Secrets → {env}.properties    │
      │  API_USERNAME / API_PASSWORD          │
      └────────┬──────────────────────────────┘
               │
      ┌────────▼──────────────────────────────┐
      │  Run API Tests                        │  (~3–5 min)
      │  mvn clean test -Denv=staging         │
      │         -Dsuite.file=SuiteSmoke.xml   │
      └────────┬──────────────────────────────┘
               │
      ┌────────▼──────────────────────────────┐
      │  Upload Artifacts                     │
      │  surefire-reports · allure-results    │
      │  logs/                                │
      └────────┬──────────────────────────────┘
               │
      ┌────────▼──────────────────────────────┐
      │  Generate Allure Report               │
      │  (keeps last 20 runs history)         │
      └────────┬──────────────────────────────┘
               │
      ┌────────▼──────────────────────────────┐
      │  Deploy to GitHub Pages               │
      │  (gh-pages branch)                    │
      └───────────────────────────────────────┘
               │
      ✅ Report live at TopPhan.github.io/...
```

### E2E User Journey Flow

```
  [Suite Setup — BaseTest.suiteSetup()]
  POST /login → Lưu VERIFY_TOKEN + COOKIES vào TokenGlobal
        │
        ▼
  [Authentication Tests — LoginTest]
  Valid login · Negative (wrong/empty creds) · Security · Schema · Performance
        │
        ▼
  [Search Tests — SearchTest]
  Keyword search · Empty keyword · Special characters · Schema validation
        │
        ▼
  [Filter Tests — FilterTest]
  Filter by category/price · Invalid params · Schema validation
        │
        ▼
  [Cart Tests — AddToCartTest]
  Add product · Invalid product ID · Missing token · Schema validation
        │
        ▼
  [Update Cart Tests — UpdateCartTest]
  Update quantity · Boundary values (0, negative, max) · Negative scenarios
        │
        ▼
  [Delete Cart Tests — DeleteItemInCartTest]
  Delete item · Delete non-existent · Verify cart state after delete
        │
        ▼
  [Assertion]
  ✅ HTTP status codes correct
  ✅ Business error_code matches expected
  ✅ Response schema validated against JSON contract
  ✅ Response time within threshold
```

---

## 🛠️ Technology Stack

| Tool | Version | Purpose |
|---|---|---|
| Java | 21 (Temurin) | Ngôn ngữ chính |
| REST Assured | 5.5.6 | HTTP client & API assertion |
| TestNG | 7.12.0 | Test runner, `@DataProvider`, listeners |
| Gson | 2.13.1 | JSON serialization / deserialization |
| Lombok | 1.18.42 | Giảm boilerplate (Builder, Getter/Setter) |
| DataFaker | 2.5.3 | Sinh test data ngẫu nhiên |
| Log4j2 | 2.25.4 | Structured logging |
| Allure TestNG | 2.27.0 | Interactive test reporting |
| Allure REST Assured | 2.25.0 | Auto-attach HTTP request/response vào Allure |
| Maven | 3.x | Build & dependency management |
| GitHub Actions | ubuntu-latest | CI/CD pipeline |

---

## 🔬 Key Technologies Explained

### REST Assured 5.5.6
HTTP client mạnh mẽ cho API testing với DSL đọc như văn xuôi. Tích hợp sẵn JSON Schema validation và filter cho Allure — mọi request/response tự động đính kèm vào test report mà không cần viết thêm code.

### ApiKeyword — Keyword-Driven Layer
Bọc REST Assured thành các method ngắn gọn (`get`, `post`, `put`, `delete`). Test case không cần biết `given/when/then` — chỉ cần gọi `ApiKeyword.post(endpoint, payload)`. Khi cần thêm header, auth scheme, hay retry logic, chỉ sửa 1 chỗ trong `ApiKeyword`.

### ResponseValidator — Centralized Assertion Engine
Tập trung toàn bộ assertion logic: HTTP status, Hasaki `error_code`, field value, field not-null, JSON Schema, response time. Khi Hasaki đổi cấu trúc response (ví dụ `status.error_code` → `meta.code`), chỉ cần cập nhật `ResponseValidator` — không động vào từng test file.

### EnvConfig — Multi-Environment Support
Singleton load đúng file `configs/{env}.properties` theo JVM flag `-Denv`. Thứ tự ưu tiên credential: JVM system property → OS environment variable → file `.properties`. Cho phép chạy local và CI mà không sửa code.

### GsonDataProvider — JSON → DataProvider Bridge
Đọc file JSON → deserialize → trả về `Object[][]` cho TestNG `@DataProvider`. Mỗi phần tử JSON là một test case độc lập với Allure result riêng, có thể enable/disable bằng cách sửa file JSON.

### JSON Schema Validation
Mỗi endpoint có file `jsonSchema/*.json` định nghĩa contract (field types, required fields). `ResponseValidator.assertSchema()` kiểm tra toàn bộ response structure — phát hiện ngay khi backend đổi API mà chưa thông báo.

### RetryAnalyzer — Flaky Test Resilience
`RetryAnalyzer` tự động retry test thất bại tối đa 2 lần. `RetryListener` wire cơ chế này vào toàn bộ test suite không cần annotate từng `@Test`. Giảm false-fail do network flakiness hoặc staging instability.

### Log4j2 — Structured Logging
Log có timestamp, endpoint, status code, response body rút gọn — lưu vào `logs/`. Giúp debug nhanh khi CI fail mà không cần reproduce local.

### Allure Report
Báo cáo tương tác với step breakdown, HTTP request/response đính kèm, timeline view, pass/fail chart, và lịch sử 20 lần chạy — tự publish lên GitHub Pages sau mỗi CI push.

---

## 📊 Data-Driven Testing

Test data được externalize hoàn toàn khỏi test code theo pattern **Data-Driven Testing (DDT)**.

**JSON files qua GsonDataProvider:**
Mỗi feature có 2 file JSON — `*Data.json` (positive) và `*InvalidData.json` (negative). Mỗi object trong mảng JSON là một test case riêng biệt, deserialize thành POJO model tương ứng.

**TestNG `@DataProvider`:**
`DataProviders.java` tập trung các `@DataProvider` method, đọc từ JSON và trả về `Object[][]`. Mỗi row là một test case độc lập với Allure result entry riêng.

```
                    ┌──────────────────┐
                    │  DataProviders   │
                    └────────┬─────────┘
             ┌───────────────┼───────────────┐
             ▼               ▼               ▼
      ┌────────────┐  ┌────────────┐  ┌────────────┐
      │  JSON      │  │  JSON      │  │  JSON      │
      │  *Data     │  │  *Invalid  │  │  Login     │
      │  .json     │  │  Data.json │  │  .json     │
      │  (Gson)    │  │  (Gson)    │  │  (Gson)    │
      └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
            └───────────────▼────────────────┘
                    ┌────────────────┐
                    │  POJO Models   │
                    │  LoginModel    │
                    │  AddToCartModel│
                    │  UpdateCart..  │
                    │  DeleteCart..  │
                    └────────┬───────┘
                             │
                    ┌────────▼────────┐
                    │  @Test methods  │
                    └─────────────────┘
```

---

## 🌐 Multi-Environment Support

Project hỗ trợ 3 môi trường độc lập, chuyển đổi bằng flag `-Denv`:

| Environment | File | Base URI | Mục đích |
|---|---|---|---|
| `dev` | `dev.properties` | `https://hasaki.vn/` | Local development (mặc định) |
| `staging` | `staging.properties` | `https://staging.hasaki.vn/` | UAT, CI pipeline |
| `prod` | `prod.properties` | `https://hasaki.vn/` | Smoke test trên production |

**EnvConfig** load đúng file theo thứ tự ưu tiên credential:
1. JVM system property: `-DUSERNAME=xxx -DPASSWORD=yyy`
2. OS environment variable: `USERNAME` / `PASSWORD`
3. File `.properties` của env đang chạy (local fallback)

```
GitHub Secrets (encrypted)
  API_USERNAME  ──┐
  API_PASSWORD  ──┤  GitHub Actions inject vào đúng {env}.properties
                  │  tại runtime (Python3 — xử lý ký tự đặc biệt an toàn)
                  ▼
         EnvConfig.getUsername() / getPassword()
                  │
                  ▼
         ConfigsGlobal.USERNAME / PASSWORD
                  │
                  ▼
         SpecBuilder → REST Assured request
```

---

## 🧪 Test Suites & Coverage

### Endpoints Được Bao Phủ

| Endpoint | Method | Test Class | Số TCs |
|---|---|---|---|
| `/mobile/v1/user/login-hasaki` | POST | `LoginTest.java` | 8 |
| `/mobile/v1/main/search` | GET | `SearchTest.java` | 14 |
| `/mobile/v2/main/products/filters` | GET | `FilterTest.java` | 14 |
| `/mobile/v1/checkout/cart/add-to-cart` | POST | `AddToCartTest.java` | 13 |
| `/mobile/v2/checkout/cart/update-product` | PUT | `UpdateCartTest.java` | 17 |
| `/mobile/v2/checkout/cart/delete-product` | DELETE | `DeleteItemInCartTest.java` | 16 |

### Test Groups

| Group | Mô tả | Số TCs |
|---|---|---|
| `positive` | Happy path — đầu vào hợp lệ | ~35 |
| `negative` | Invalid input, error handling | ~25 |
| `security` | CSRF, auth bypass, brute-force lockout | ~8 |
| `schema` | JSON Schema contract validation | ~6 |
| `performance` | Response time threshold | ~6 |
| `smoke` | BLOCKER/CRITICAL — chạy sau mỗi build | ~12 |

### XML Suite Structure

```
Suites
├── SuiteSmoke.xml          ← ~2–3 min | BLOCKER checks | groups: smoke
├── SuiteSanity.xml         ← Core positive scenarios | groups: positive
├── SuiteRegression.xml     ← Full regression: positive + negative + schema
├── SuiteFull.xml           ← Tất cả groups: tất cả 82 test cases
├── SuiteNegative.xml       ← Chỉ groups: negative + security
├── SuitePerformance.xml    ← Chỉ groups: performance
├── SuiteSecurity.xml       ← Chỉ groups: security
├── SuiteDataDriven.xml     ← Data-driven positive + negative
└── SuiteEndToEnd.xml       ← Full user journey: Login → Search → Filter → Cart
    ├── Journey 1: Login → Search → Filter → Add → Update → Delete
    └── Journey 2: Login fail → Login success → Search → Add → Delete
```

---

## 📈 Allure Report

**Report URL:** [https://TopPhan.github.io/Phan_Hoang_Dinh_API_Testing_Hasaki.vn/](https://TopPhan.github.io/Phan_Hoang_Dinh_API_Testing_Hasaki.vn/)

**What the report shows:**
- Pass / Fail / Skip summary per test
- Step-by-step breakdown với HTTP request & response đính kèm tự động
- Allure `@Epic`, `@Feature`, `@Story`, `@Description`, `@Severity` cho mỗi test
- Historical trend chart (last 20 CI runs)
- Environment metadata: Java version, OS, Base URI, Active Env, Tester

---

## ⚙️ CI/CD Pipeline — GitHub Actions

Pipeline định nghĩa trong `.github/workflows/API_Testing.yml`. Trigger khi:
- **Push** lên `master` / `main`
- **Pull Request** vào `master` / `main`
- **Manual dispatch** — chọn suite và environment qua GitHub UI

```yaml
inputs:
  suite:  # VD: SuiteSmoke.xml / SuiteFull.xml / SuiteRegression.xml
  env:    # dev / staging / prod  (mặc định: staging)

jobs:
  API_Test:
    runs-on: ubuntu-latest
    steps:
      - Checkout code
      - Set up JDK 21
      - Inject credentials into {env}.properties   # ← Python3 inject secrets
      - Run API Tests (mvn clean test -Denv=... -Dsuite.file=...)
      - Upload Test Artifacts (surefire · allure-results · logs)
      - Generate Allure Report (keeps 20 runs)
      - Deploy to GitHub Pages
```

---

## 🔐 Security — Credential Management

Toàn bộ credentials được quản lý qua **GitHub Actions Secrets** — không có email hay password nào lưu trong source code.

### How it works

```
GitHub Secrets (encrypted)
  API_USERNAME  ──┐
  API_PASSWORD  ──┤  Inject vào đúng file env tại runtime
                  ▼
        Python3 đọc {env}.properties
        Replace dòng USERNAME / PASSWORD
        (chỉ trong runner memory, không commit)
```

File `staging.properties` luôn để trống credentials:

```properties
USERNAME =
PASSWORD =
```

CI workflow inject real values bằng Python (an toàn với ký tự đặc biệt `@`, `!`, `/`):

```yaml
- name: Inject credentials into env config
  env:
    API_USERNAME: ${{ secrets.CHROME_USER }}
    API_PASSWORD: ${{ secrets.CHROME_PASS }}
    TARGET_ENV:   ${{ github.event.inputs.env || 'staging' }}
  run: |
    python3 - << 'PYEOF'
    import os
    env      = os.environ.get("TARGET_ENV", "staging").lower()
    username = os.environ["API_USERNAME"]
    password = os.environ["API_PASSWORD"]

    props_path = f"src/test/resources/configs/{env}.properties"
    with open(props_path, "r", encoding="utf-8") as f:
        content = f.read()
    lines = []
    for line in content.splitlines():
        if line.strip().startswith("USERNAME"):
            lines.append(f"USERNAME = {username}")
        elif line.strip().startswith("PASSWORD"):
            lines.append(f"PASSWORD = {password}")
        else:
            lines.append(line)
    with open(props_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    PYEOF
```

### Setting up GitHub Secrets

1. Vào repository → **Settings → Secrets and variables → Actions**
2. Click **New repository secret** và thêm:

| Secret Name | Description |
|---|---|
| `CHROME_USER` | Email đăng nhập tài khoản test |
| `CHROME_PASS` | Password tài khoản test |

### Running locally

**Windows (Command Prompt as Admin):**
```cmd
setx CHROME_USER "your_email@gmail.com"
setx CHROME_PASS "your_password"
```
> Restart IntelliJ IDEA sau `setx` để biến môi trường có hiệu lực.

**macOS / Linux:**
```bash
export CHROME_USER="your_email@gmail.com"
export CHROME_PASS="your_password"
```

---

## ⚙️ Configuration & How to Run the Project

### Prerequisites

- Java 21+ (`java -version`)
- Maven 3.6+ (`mvn -version`)
- Internet access đến [https://hasaki.vn/](https://hasaki.vn/) hoặc staging URL
- Environment variables `CHROME_USER` / `CHROME_PASS` (xem mục Security)

### 1. Clone the Repository

```bash
git clone https://github.com/TopPhan/Phan_Hoang_Dinh_API_Testing_Hasaki.vn.git
cd Phan_Hoang_Dinh_API_Testing_Hasaki.vn
```

### 2. Configure Environment (Optional)

Chỉnh sửa file tương ứng trong `src/test/resources/configs/`:

```properties
# dev.properties
BASE_URI      = https://hasaki.vn/
USERNAME      = your_email@gmail.com   # hoặc để trống, dùng env var
PASSWORD      = your_password

# Timeout (milliseconds)
CONNECTION.TIMEOUT = 10000
READ.TIMEOUT       = 15000
```

### 3. Run Test Suites

**Chạy Smoke Suite (nhanh nhất — ~2-3 min):**
```bash
mvn clean test -Denv=staging -Dsuite.file=suites/SuiteSmoke.xml
```

**Chạy Full Regression:**
```bash
mvn clean test -Denv=staging -Dsuite.file=suites/SuiteRegression.xml
```

**Chạy tất cả test cases:**
```bash
mvn clean test -Denv=staging -Dsuite.file=suites/SuiteFull.xml
```

**Chạy E2E User Journey:**
```bash
mvn clean test -Denv=staging -Dsuite.file=suites/SuiteEndToEnd.xml
```

**Chạy Negative tests:**
```bash
mvn clean test -Denv=staging -Dsuite.file=suites/SuiteNegative.xml
```

**Chạy Security tests:**
```bash
mvn clean test -Denv=staging -Dsuite.file=suites/SuiteSecurity.xml
```

**Chạy trên Production (Smoke only):**
```bash
mvn clean test -Denv=prod -Dsuite.file=suites/SuiteSmoke.xml
```

### 4. View Allure Report Locally

```bash
# Generate và mở report tương tác
allure serve allure-results

# Hoặc generate static report folder
allure generate allure-results --clean -o allure-report
```

---

## 📁 Output Artifacts

| Path | Content |
|---|---|
| `allure-results/` | Raw JSON result files cho Allure |
| `logs/` | Log4j2 runtime log với timestamp và endpoint |
| `target/surefire-reports/` | Maven Surefire XML & HTML reports |

---

## 👤 Author

**Phan Hoang Dinh** — Automation Test Engineer  
[GitHub Profile](https://github.com/topphan)

---

*Framework này nhắm vào live [Hasaki.vn](https://hasaki.vn/) website cho mục đích demo portfolio.*
