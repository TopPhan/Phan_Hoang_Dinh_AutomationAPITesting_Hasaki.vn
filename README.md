![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![REST Assured](https://img.shields.io/badge/REST--Assured-5.x-green?style=flat-square&logo=java)
![TestNG](https://img.shields.io/badge/TestNG-7.x-red?style=flat-square)
![Maven](https://img.shields.io/badge/Maven-Project-blue?style=flat-square&logo=apache-maven) <br>
![API Testing](https://github.com/TopPhan/Phan_Hoang_Dinh_AutomationAPITesting_Hasaki.vn/actions/workflows/API_Testing.yml/badge.svg)

# 🌿 [Hasaki.vn](https://hasaki.vn/) — API Testing Framework

<!-- PROJECT SCREENSHOT PLACEHOLDER -->
> <img width="1885" height="841" alt="image" src="https://github.com/user-attachments/assets/4eb3cf3e-fc3b-43dd-9dc4-4d24c2ca7cd1" />

> <img width="1919" height="839" alt="image" src="https://github.com/user-attachments/assets/a41e532c-f947-4d32-be6d-ae7ff15d53f6" />

> <img width="1895" height="868" alt="image" src="https://github.com/user-attachments/assets/27d5cc4b-6d22-453d-9e6f-5a4b41bf7e0d" />

> <img width="1911" height="1024" alt="image" src="https://github.com/user-attachments/assets/aa51a2eb-60d3-489d-bbb1-6f193df1d76d" />

> <img width="1919" height="1023" alt="image" src="https://github.com/user-attachments/assets/bd12d115-28c1-4bd9-822b-4721fd3d5cdf" />






---

[![Allure Report](https://img.shields.io/badge/Allure%20Report-View%20Here-ff69b4?style=for-the-badge&logo=allure)](https://topphan.github.io/Phan_Hoang_Dinh_AutomationAPITesting_Hasaki.vn)

---

## 📋 Table of Contents

- [Project Overview](#-project-overview)
- [Tech Stack](#%EF%B8%8F-technology-stack)
- [Project Structure](#-project-structure)
- [Architecture & Flow Diagrams](#-architecture--flow-diagrams)
- [Key Technologies Explained](#-key-technologies-explained)
- [API Endpoints Under Test](#-api-endpoints-under-test)
- [Test Suites & Coverage](#-test-suites--coverage)
- [Data-Driven Testing](#-data-driven-testing)
- [Multi-Environment Support](#-multi-environment-support)
- [Security — GitHub Secrets](#-security--credential-management)
- [How to Run](#-how-to-run-the-project)
- [Allure Report](#-allure-report)
- [CI/CD Pipeline](#%EF%B8%8F-cicd-pipeline--github-actions)

---

## 🌿 Project Overview

**Hasaki.vn API Testing** is a production-grade REST API test automation framework targeting the backend APIs of [Hasaki.vn](https://hasaki.vn/) — Vietnam's leading beauty & skincare e-commerce platform.  
Built with **REST-Assured**, **TestNG**, and a layered keyword-driven architecture, this framework validates the full API surface across Authentication, Search, Filter, and Cart operations — covering positive, negative, security, schema, performance, and end-to-end scenarios — with automated Allure reporting deployed to GitHub Pages on every CI run.

| Metric | Result |
|---|---|
| 🧪 Total Test Cases | **90 tests** (Smoke · Regression · Negative · Security · Performance · E2E) |
| 🔗 API Endpoints Covered | **6 endpoints** across 3 feature domains |
| 🌍 Environments | **dev · staging · prod** (switchable via `-Denv`) |
| 📈 Allure Report | Auto-deployed to **GitHub Pages** — keeps last 20 runs |
| 🤖 CI Platform | **GitHub Actions (Ubuntu Latest)** |
| 🔐 Credential Security | **GitHub Secrets** — zero hardcoded credentials in source |
| 🔁 Flaky Test Resilience | **RetryAnalyzer** — auto-retries up to 2 times on network/session failures |

### 🌟 Key Strengths

- **Layered keyword architecture** — `ApiKeyword` wraps all REST-Assured verbs; tests stay clean and readable without any HTTP boilerplate.
- **Centralized response validation** — `ResponseValidator` owns all assertions (HTTP status, Hasaki `error_code`, field presence, field values). Update once; all tests benefit.
- **JSON Schema validation** — Response contracts enforced via REST-Assured's JSON Schema Validator, catching silent API breaking changes automatically.
- **Data-driven testing** — All test data externalized to JSON files via `GsonDataProvider`, enabling scenario expansion without any code changes.
- **Smart session management** — `BaseTest` handles the complex Hasaki auth flow (`form_key` + `HSKSIGN` + `HASAKI_SESSID` + `verify_token`) transparently. Downstream tests never deal with auth plumbing.
- **Multi-environment config** — Switch between `dev`, `staging`, and `prod` with a single `-Denv` flag. Credential injection at runtime from GitHub Secrets — no sensitive data in source.
- **Auto retry on flakiness** — `RetryAnalyzer` + `RetryListener` automatically retry failing tests caused by network or session timeouts (max 2 retries), distinguishing environment errors from logic failures.
- **Live Allure reporting** — Every CI run auto-publishes an interactive report to GitHub Pages with step breakdowns, response logs, and 20-run trend history.

---

## 🛠️ Technology Stack

| Category | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| HTTP Client / Test DSL | REST-Assured | 5.x |
| Test Runner | TestNG | 7.x |
| Build Tool | Maven | 3.x |
| Serialization | Gson | 2.13.1 |
| POJO Boilerplate | Lombok | 1.18.42 |
| Test Data Generation | DataFaker | 2.5.3 |
| Logging | Log4j2 + SLF4J | 2.25.4 / 2.0.17 |
| Reporting | Allure TestNG + Allure REST-Assured | 2.x |
| CI/CD | GitHub Actions | ubuntu-latest |

---

## 🧱 Project Structure

```
Hasaki.vn_API_Testing/
├── .github/
│   └── workflows/
│       └── API_Testing.yml              # GitHub Actions CI/CD pipeline
│
├── src/
│   ├── main/
│   │   ├── java/com/
│   │   │   ├── globals/
│   │   │   │   ├── ConfigsGlobal.java   # Runtime config values loaded from EnvConfig
│   │   │   │   ├── EndPointGlobal.java  # All API endpoint constants
│   │   │   │   ├── EnvConfig.java       # Multi-env config loader (-Denv=dev/staging/prod)
│   │   │   │   └── TokenGlobal.java     # Global session state (cookies, tokens)
│   │   │   ├── helper/
│   │   │   │   ├── GsonDataProvider.java # JSON → Object[][] DataProvider engine
│   │   │   │   ├── JsonHelper.java       # JSON read/write utilities
│   │   │   │   ├── LogUtils.java         # Log4j2 logger wrapper
│   │   │   │   ├── PropertiesHelper.java # .properties file reader
│   │   │   │   ├── SchemaHelper.java     # JSON Schema loader
│   │   │   │   └── SystemHelper.java     # JVM/OS system utilities
│   │   │   ├── keywords/
│   │   │   │   ├── ApiKeyword.java       # Core HTTP verbs (GET, POST, PUT, DELETE)
│   │   │   │   ├── GetSection.java       # Session bootstrap (getCookies, form_key)
│   │   │   │   └── SpecBuilder.java      # REST-Assured request/response spec factory
│   │   │   ├── listener/
│   │   │   │   ├── RetryAnalyzer.java    # Flaky-test retry logic (max 2 retries)
│   │   │   │   ├── RetryListener.java    # Auto-applies RetryAnalyzer to all suite tests
│   │   │   │   └── TestListener.java     # Allure environment info + test lifecycle logging
│   │   │   ├── reports/
│   │   │   │   └── AllureManager.java    # Allure step/log helper
│   │   │   └── validator/
│   │   │       └── ResponseValidator.java # Central assertion engine for all API responses
│   │   └── resources/
│   │       ├── log4j2.properties
│   │       └── TutorialUse/             # Usage guides for GsonDataProvider & JsonHelper
│   │
│   └── test/
│       └── java/com/
│           ├── baseSetup/
│           │   └── BaseTest.java         # Suite-level auth setup & per-class session refresh
│           ├── builder/
│           │   └── LoginPOJO_Builder.java # Builds login request body from config
│           ├── pojoModel/
│           │   ├── AddToCartModel.java
│           │   ├── DeleteCartModel.java
│           │   ├── LoginModel.java
│           │   └── UpdateCartModel.java
│           ├── testScenarios/
│           │   ├── LoginTest.java         # 8 tests  — Authentication
│           │   ├── SearchTest.java        # 14 tests — Product Search
│           │   ├── FilterTest.java        # 14 tests — Product Filter
│           │   ├── AddToCartTest.java     # 13 tests — Add to Cart
│           │   ├── UpdateCartTest.java    # 17 tests — Update Cart
│           │   └── DeleteItemInCartTest.java # 16 tests — Delete from Cart
│           └── dataProvider/
│               └── DataProviders.java     # All @DataProvider methods (JSON-driven)
│
└── src/test/resources/
    ├── configs/
    │   ├── dev.properties
    │   ├── staging.properties
    │   └── prod.properties
    ├── jsonData/                          # Test input data (valid & invalid)
    │   ├── Login.json / LoginInvalidData.json
    │   ├── SearchData.json / SearchInvalidData.json
    │   ├── FilterData.json
    │   ├── AddToCartData.json / AddToCartInvalidData.json
    │   ├── UpdateCartData.json / UpdateCartInvalidData.json
    │   └── DeleteCartData.json / DeleteCartInvalidData.json
    ├── jsonSchema/                        # JSON Schema contracts
    │   ├── LoginSchema.json
    │   ├── SearchSchema.json
    │   ├── FilterSchema.json
    │   └── AddToCartSchema.json
    └── suites/
        ├── SuiteSmoke.xml
        ├── SuiteRegression.xml
        ├── SuiteNegative.xml
        ├── SuiteSecurity.xml
        ├── SuitePerformance.xml
        ├── SuiteDataDriven.xml
        ├── SuiteSanity.xml
        ├── SuiteEndToEnd.xml
        └── SuiteFull.xml
```

---

## 🔄 Architecture & Flow Diagrams

### Keyword-Driven API Testing Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    TEST SCENARIO LAYER                  │
│  LoginTest │ SearchTest │ FilterTest │ Cart Tests ...   │
└────────────────────────┬────────────────────────────────┘
                         │ calls
┌────────────────────────▼────────────────────────────────┐
│                  KEYWORD / VALIDATOR LAYER              │
│  ApiKeyword │ ResponseValidator │ GetSection            │
└────────────────────────┬────────────────────────────────┘
                         │ uses
┌────────────────────────▼────────────────────────────────┐
│                    SPEC / GLOBAL LAYER                  │
│  SpecBuilder │ ConfigsGlobal │ TokenGlobal              │
└────────────────────────┬────────────────────────────────┘
                         │ drives
┌────────────────────────▼────────────────────────────────┐
│              REST-ASSURED HTTP CLIENT LAYER             │
│           hasaki.vn Live API (staging / prod)           │
└─────────────────────────────────────────────────────────┘
```

### Session Management Flow (BaseTest)

```
  Suite Start (@BeforeSuite)
        │
        ▼
  GetSection.getCookies()              ← Fetch form_key + HSKSIGN
        │
        ▼
  ApiKeyword.post(EP_LOGIN, body)      ← Authenticate
        │
        ▼
  errorCode == 0?
    ├── YES → mergeCookies()           ← Store HASAKI_SESSID
    │         extractPostLoginTokens() ← Store VERIFY_TOKEN, HSKSIGN
    │
    └── NO  → LogUtils.error()

  Per Class (@BeforeClass)
        │
        ▼
  GetSection.getCookies()              ← Refresh short-lived form_key + HSKSIGN
        │
        ▼
  Has HASAKI_SESSID + VERIFY_TOKEN?
    ├── YES → Use existing session     ← Avoid device-verify trigger
    └── NO  → Re-login fallback
```

### CI/CD Pipeline Flow

```
  Push to master / PR / Manual Dispatch
              │
              ▼
      ┌───────────────────┐
      │  Checkout Code    │
      └────────┬──────────┘
               │
      ┌────────▼──────────┐
      │  Set up JDK 21    │
      └────────┬──────────┘
               │
      ┌────────▼──────────────────────────────┐
      │  Inject Credentials (Python3)          │
      │  GitHub Secrets → {env}.properties    │
      │  USERNAME / PASSWORD                   │
      └────────┬───────────────────────────────┘
               │
      ┌────────▼────────────────────────┐
      │  Run Maven Tests                │
      │  -Denv={env}                    │
      │  -Dsuite.file=suites/{suite}    │
      └────────┬────────────────────────┘
               │
      ┌────────▼────────────────────────┐
      │  Upload Artifacts               │
      │  surefire-reports/              │
      │  allure-results/ · logs/        │
      └────────┬────────────────────────┘
               │
      ┌────────▼────────────────────────┐
      │  Generate Allure Report         │
      │  (keep last 20 runs)            │
      └────────┬────────────────────────┘
               │
      ┌────────▼────────────────────────┐
      │  Deploy to GitHub Pages         │
      │  → gh-pages branch              │
      └─────────────────────────────────┘
```

---

## 🔬 Key Technologies Explained

### REST-Assured 5.x
Core HTTP client and test DSL. `ApiKeyword.java` wraps all verbs (`GET`, `POST`, `PUT`, `DELETE`) with Allure `@Step` annotations, LogUtils logging, and the shared `SpecBuilder` — so every test call is one line of code, not a dozen lines of configuration.

### SpecBuilder — Three Request Spec Variants
Centralised REST-Assured spec factory providing three modes: **authenticated JSON** (cookies + `form_key` + `verify_token`), **form-urlencoded** (for server endpoints requiring it), and **unauthenticated** (for negative and security tests). Switching spec means switching one method call.

### ResponseValidator — Centralized Assertion Engine
All assertion logic lives in one class: HTTP status, Hasaki business `error_code` (0 = success), field equality, field non-empty, list size, and response time thresholds. When the API contract changes, update `ResponseValidator` once — no test file modifications required.

### JSON Schema Validation
`SchemaHelper` + REST-Assured's `json-schema-validator` enforces response contracts at the structure level. Schema files in `jsonSchema/` describe the exact shape of each API response. Any missing field or type mismatch fails the test immediately — catching silent breaking changes before they reach production.

### BaseTest — Smart Session Management
Hasaki's API requires a complex multi-token auth chain (`form_key`, `HSKSIGN`, `HASAKI_SESSID`, `verify_token`). `BaseTest` handles this transparently with a `@BeforeSuite` full login and per-class `@BeforeClass` token refresh strategy — avoiding the device-verify trigger while keeping all downstream tests authenticated.

### GsonDataProvider — JSON-to-DataProvider Engine
`GsonDataProvider.loadFromResource()` reads any JSON array file and converts it to `Object[][]` for TestNG `@DataProvider`. Tests simply declare which JSON file and which fields they need — no parsing code in test classes.

### RetryAnalyzer + RetryListener
`RetryAnalyzer` distinguishes environment errors (network timeout, 5xx, session expiry) from logic failures (assertion mismatch). Only environment errors trigger a retry, up to `MAX_RETRY = 2`. `RetryListener` applies this automatically to every test in the suite without requiring `retryAnalyzer` annotation on each `@Test`.

### Log4j2 — Structured Logging
Timestamped runtime logs written to `logs/` alongside Allure step logs embedded directly in the report — providing two levels of debugging: quick Allure review for step failures, deep log file analysis for session/network issues.

---

## 🔗 API Endpoints Under Test

| Endpoint | Method | Description |
|---|---|---|
| `/mobile/v1/user/login-hasaki` | `POST` | User authentication — returns `verify_token` + session cookies |
| `/mobile/v1/main/search` | `GET` | Product search by keyword with pagination |
| `/mobile/v2/main/products/filters` | `GET` | Product filter by category, price range, attributes |
| `/mobile/v1/checkout/cart/add-to-cart` | `POST` | Add a product to the shopping cart |
| `/mobile/v2/checkout/cart/update-product` | `POST` | Update product quantity in cart |
| `/mobile/v2/checkout/cart/delete-product` | `POST` | Delete a product from cart |

---

## 🧪 Test Suites & Coverage

| Module | Test Class | Test Count | Scenarios Covered |
|---|---|---|---|
| 🔑 Authentication | `LoginTest.java` | **8 tests** | Valid login, remember flag, invalid credentials, empty fields, CSRF enforcement, brute-force lockout, schema validation, response time |
| 🔍 Search | `SearchTest.java` | **14 tests** | Valid keyword, pagination, special characters, empty keyword, SQL injection, schema validation, response time |
| 🎛️ Filter | `FilterTest.java` | **14 tests** | Valid category filter, price range, combined filters, invalid params, boundary values, schema validation, response time |
| 🛒 Add to Cart | `AddToCartTest.java` | **13 tests** | Add valid product, add without auth, invalid product ID, quantity boundaries, duplicate add, schema validation |
| ✏️ Update Cart | `UpdateCartTest.java` | **17 tests** | Update quantity, max/min boundary, invalid item ID, zero quantity, negative quantity, without auth |
| 🗑️ Delete from Cart | `DeleteItemInCartTest.java` | **16 tests** | Delete valid item, delete non-existent item, delete without auth, delete already-deleted item |
| **Total** | **6 test classes** | **90 tests** | |

### Test Groups

| Group | Purpose | Suites Using It |
|---|---|---|
| `smoke` | BLOCKER/CRITICAL paths only — fast health check | SuiteSmoke |
| `positive` | Happy path scenarios | SuiteRegression, SuiteEndToEnd |
| `negative` | Invalid inputs, error responses | SuiteNegative, SuiteRegression |
| `schema` | JSON contract validation | SuiteRegression, SuiteSanity |
| `security` | CSRF, brute-force, unauthorized access | SuiteSecurity |
| `performance` | Response time thresholds | SuitePerformance |
| `datadriven` | Data-driven iteration over JSON datasets | SuiteDataDriven |

### XML Suite Structure

```
suites/
├── SuiteSmoke.xml          ← BLOCKER smoke tests only (~2–3 min)
├── SuiteRegression.xml     ← positive + negative + schema (~15–25 min)
├── SuiteNegative.xml       ← negative & boundary tests
├── SuiteSecurity.xml       ← CSRF, brute-force, unauthorized
├── SuitePerformance.xml    ← response time thresholds
├── SuiteDataDriven.xml     ← full JSON data-driven pass
├── SuiteSanity.xml         ← sanity checks (schema + smoke)
├── SuiteEndToEnd.xml       ← full user journeys (2 complete flows)
│     Journey 1: Login → Search → Filter → Add to Cart → Update → Delete
│     Journey 2: Failed Login → Retry Login → Search → Add → Delete
└── SuiteFull.xml           ← all tests, all groups
```

---

## 📊 Data-Driven Testing

Test data is fully externalized from test code following the **Data-Driven Testing (DDT)** pattern via JSON files and `GsonDataProvider`.

**JSON Data Files (`jsonData/`):**
Each feature has a `*Data.json` (valid/positive cases) and `*InvalidData.json` (negative cases). Each entry includes a `testCaseId` and `description` for clear Allure report labeling.

**`GsonDataProvider.loadFromResource()`:**
Reads any JSON array file and maps specified field names to `Object[][]` for seamless integration with TestNG `@DataProvider`. Zero parsing boilerplate in test classes.

**`DataProviders.java` — Centralized Data Hub:**
All `@DataProvider` methods live in one class, acting as a clear inventory of every data source in the project.

```
                    ┌────────────────────┐
                    │   DataProviders    │
                    └─────────┬──────────┘
          ┌──────────────────┼──────────────────┐
          ▼                  ▼                  ▼
  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
  │ Login*.json  │  │ Search*.json │  │  Cart*.json       │
  │ (Credentials │  │ (Keywords,   │  │  (productId, qty, │
  │  edge cases) │  │  pagination) │  │  invalid params)  │
  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘
         └──────────────────▼──────────────────┘
                    ┌────────────────────┐
                    │  GsonDataProvider  │
                    │  loadFromResource()│
                    └─────────┬──────────┘
                              │
                    ┌─────────▼──────────┐
                    │   POJO Models      │
                    │   LoginModel       │
                    │   AddToCartModel   │
                    │   UpdateCartModel  │
                    │   DeleteCartModel  │
                    └─────────┬──────────┘
                              │
                    ┌─────────▼──────────┐
                    │   @Test methods    │
                    └────────────────────┘
```

---

## 🌍 Multi-Environment Support

The framework supports **three environments** switchable at runtime via `-Denv`:

| Environment | Config File | Base URI | Use Case |
|---|---|---|---|
| `dev` (default) | `configs/dev.properties` | `https://hasaki.vn/` | Local development |
| `staging` | `configs/staging.properties` | `https://staging.hasaki.vn/` | Pre-release QA |
| `prod` | `configs/prod.properties` | `https://hasaki.vn/` | Production smoke |

**Credential Priority (highest → lowest):**
1. JVM system property: `-DUSERNAME=xxx -DPASSWORD=yyy` (CI / CLI override)
2. Environment variable: `USERNAME` / `PASSWORD` (OS / GitHub Secrets)
3. Value in the active `.properties` file (local fallback)

---

## 🔐 Security — Credential Management

All account credentials are injected at runtime from **GitHub Secrets** — no sensitive values are ever stored in source code or `.properties` files.

The CI pipeline injects secrets into the target environment's `.properties` file via a Python3 script before Maven runs:

```
GitHub Secrets
  CHROME_USER  →  USERNAME in {env}.properties
  CHROME_PASS  →  PASSWORD in {env}.properties
```

The injection script targets only the `USERNAME` and `PASSWORD` lines, leaving all other config intact. Credentials are never printed to CI logs.

---

## ⚙️ How to Run the Project

### Prerequisites

- Java 21+
- Maven 3.x
- Allure CLI (optional, for local report generation)

### 1. Clone the Repository

```bash
git clone https://github.com/TopPhan/Hasaki.vn_API_Testing.git
cd Hasaki.vn_API_Testing
```

### 2. Configure Credentials

Add your Hasaki.vn account credentials to the target environment's `.properties` file, or pass them as system properties:

```bash
# Option A — Edit .properties file
# src/test/resources/configs/dev.properties
USERNAME = your_email@example.com
PASSWORD = your_password

# Option B — System properties (no file edit required)
mvn clean test -DUSERNAME=your_email@example.com -DPASSWORD=your_password
```

### 3. Run Tests

**Default (dev env, SuiteSmoke):**
```bash
mvn clean test
```

**Specify environment:**
```bash
mvn clean test -Denv=staging
mvn clean test -Denv=prod
```

**Specify suite:**
```bash
mvn clean test -Dsuite.file=suites/SuiteSmoke.xml
mvn clean test -Dsuite.file=suites/SuiteRegression.xml
mvn clean test -Dsuite.file=suites/SuiteNegative.xml
mvn clean test -Dsuite.file=suites/SuiteSecurity.xml
mvn clean test -Dsuite.file=suites/SuitePerformance.xml
mvn clean test -Dsuite.file=suites/SuiteEndToEnd.xml
mvn clean test -Dsuite.file=suites/SuiteFull.xml
```

**Combine environment + suite:**
```bash
mvn clean test -Denv=staging -Dsuite.file=suites/SuiteRegression.xml
```

**Run a single test class:**
```bash
mvn clean test -Dtest=LoginTest
```

### 4. View Allure Report Locally

```bash
# Generate and open interactive report
allure serve allure-results

# Or generate static report folder
allure generate allure-results --clean -o allure-report
```

---

## 📈 Allure Report

**Report URL:** *(Will be published to GitHub Pages after CI/CD setup)*

**What the report shows:**
- Pass / Fail / Skip summary per test, grouped by Feature and Story
- Step-by-step breakdown of each API call with request/response bodies logged inline
- Historical trend chart (last 20 CI runs)
- Failure details with exact assertion messages
- Environment metadata: Java version, OS, target environment, suite name, author

**Allure Annotations used in this project:**

| Annotation | Usage |
|---|---|
| `@Epic` | Top-level grouping — "Hasaki.vn API Testing" |
| `@Feature` | Feature domain — "Authentication", "Search", "Cart", etc. |
| `@Story` | Individual test case ID + title (e.g. "TC-L01: Valid credentials") |
| `@Description` | Full test intent description |
| `@Severity` | `BLOCKER`, `CRITICAL`, `NORMAL`, `MINOR` |
| `@Step` | Inline step labels on all `ApiKeyword` and `ResponseValidator` methods |

---

## ⚙️ CI/CD Pipeline — GitHub Actions

Pipeline defined in `.github/workflows/API_Testing.yml`. Triggers on:
- **Push** to `master` / `main`
- **Pull Request** to `master` / `main`
- **Manual dispatch** via GitHub UI — with selectable `suite` and `env` inputs

```yaml
jobs:
  API_Test:
    runs-on: ubuntu-latest
    steps:
      - Checkout code
      - Set up JDK 21
      - Inject credentials into {env}.properties   # Python3 replaces USERNAME/PASSWORD
      - Run Maven Tests                            # -Denv + -Dsuite.file
      - Upload Artifacts                           # surefire-reports, allure-results, logs
      - Generate Allure Report                     # keeps last 20 runs
      - Deploy to GitHub Pages                     # → gh-pages branch
```

**Manual Dispatch Inputs:**

| Input | Default | Options |
|---|---|---|
| `suite` | `SuiteSmoke.xml` | Any suite file in `suites/` |
| `env` | `staging` | `dev`, `staging`, `prod` |

---

## 📁 Output Artifacts

| Path | Content |
|---|---|
| `allure-results/` | Raw JSON result files for Allure report generation |
| `logs/` | Full Log4j2 runtime logs per test run |
| `target/surefire-reports/` | Maven Surefire XML & HTML reports |

---

## 👤 Author

**Phan Hoang Dinh** — Automation Test Engineer  
[GitHub Profile](https://github.com/topphan)

---

*This framework targets the live [Hasaki.vn](https://hasaki.vn/) APIs for portfolio demonstration purposes.*

