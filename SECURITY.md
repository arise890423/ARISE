# ARISE Security Architecture & Audit Report

## 1. Executive Summary
This document provides a comprehensive security audit, architecture blueprint, and hardening guide for the ARISE Solo Leveling-inspired Android productivity and gamification platform.

All security enhancements have been implemented directly in the codebase and verified through automated tests.

---

## 2. Secrets Audit & Credential Hygiene
A repository-wide audit was conducted across Kotlin source files, Gradle configurations, XML resources, and asset bundles.

### Audit Findings:
- **No Private AI or Cloud Provider Secrets in Client**: The client application contains zero hardcoded Gemini API keys, OpenAI keys, or cloud database credentials.
- **`google-services.json`**: Contains client identifiers (`project_number`, `project_id`, `client_id`, `api_key`). In Firebase mobile architecture, these values identify the client app and are not private backend secrets. Security is enforced strictly via **Firestore Security Rules**, **Firebase Storage Rules**, and **Firebase Authentication**.
- **`metadata.json`**: Maintains `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` to enforce that generative AI processing is executed server-side.
- **`.env.example`**: Secrets placeholders are strictly commented out and documented for server deployment environments.

> **CRITICAL REALITY CHECK**: Storing secrets in environment variables or Gradle build configs (`BuildConfig`) does **NOT** hide them from client-side APK extraction. Any string or key embedded in a distributed Android binary can be decompiled or retrieved via reverse-engineering tools (e.g., `jadx`, `apktool`, Frida). Therefore, all privileged operations, payment verifications, and AI API keys must reside exclusively on trusted server infrastructure.

---

## 3. Network & Transport Security
- **Strict HTTPS Enforcement**: Android cleartext traffic is explicitly forbidden (`android:usesCleartextTraffic="false"`).
- **Network Security Configuration**: `res/xml/network_security_config.xml` mandates TLS for all domains and specifies system trust anchors:
  ```xml
  <network-security-config>
      <base-config cleartextTrafficPermitted="false">
          <trust-anchors>
              <certificates src="system" />
          </trust-anchors>
      </base-config>
  </network-security-config>
  ```
- **Certificate Validation**: No custom `TrustManager` or certificate-pinning bypasses exist. Standard Android system PKI certificate validation is strictly maintained.

---

## 4. Authentication, Session & Credential Storage
- **Cryptographic Password Hashing**: Passwords stored on the device for local offline session management are never held in plaintext. They are protected using **PBKDF2 with HMAC-SHA256**:
  - 16-byte cryptographically secure random salt generated via `SecureRandom`.
  - 10,000 iterations.
  - 256-bit derived key length.
  - Salt and hash stored in standard format (`pbkdf2_sha256$<iterations>$<salt>$<hash>`).
- **Timing-Attack Resistance**: Verification uses constant-time byte comparison (`MessageDigest.isEqual`) to defeat side-channel timing analysis.
- **Brute-Force & Credential Stuffing Mitigation**:
  - The `SecurityAuditManager` tracks failed authentication attempts per account identifier.
  - Exceeding **5 failed attempts within a 5-minute window** triggers an immediate **60-second account lockout**.
  - Authentication failure responses return uniform, generic error messages to prevent account enumeration.
- **Data Backup & Extraction Hardening**:
  - `res/xml/data_extraction_rules.xml` and `res/xml/backup_rules.xml` exclude sensitive user preference files (`arise_secure_prefs.xml`, `arise_hunter_prefs.xml`) from cloud backups and device-to-device ADB transfers.
- **ProGuard / R8 Log Stripping**:
  - `proguard-rules.pro` configures R8 in release builds to strip `android.util.Log` debug, verbose, and info calls, preventing unintentional PII and token exposure in logcat.

---

## 5. Database & Cloud Storage Security Rules

### Firestore Zero-Trust Rules (`firestore.rules`)
Firestore security rules enforce least-privilege, owner-isolated data access:
1. **User Profiles (`/users/{userId}`)**:
   - Authenticated users may read profiles.
   - Users may update non-privileged fields (e.g., display name, bio, equipped cosmetics).
   - Users **CANNOT** modify their own `gold`, `crystals`, `xp`, `currentXp`, `admin`, or `role` fields:
     ```javascript
     !request.resource.data.diff(resource.data).affectedKeys()
       .hasAny(['gold', 'crystals', 'xp', 'currentXp', 'admin', 'role'])
     ```
2. **Community Chat & Direct Messaging (`/community_rooms/{roomId}`)**:
   - Public rooms (`GLOBAL`, `STRATEGY`, `TRADING`, `RAIDS`) are readable by all authenticated users.
   - Private and group rooms (`DIRECT`, `GROUP`) are strictly readable and writable only by users present in `resource.data.memberIds`.
   - Message creation validates sender identity (`request.resource.data.senderId == request.auth.uid`) and content bounds (`content.size() <= 1000`).
3. **Purchases (`/purchases/{purchaseId}`)**:
   - Read-only for the owning purchaser (`resource.data.userId == request.auth.uid`).
   - Client writes are completely disabled (`allow write: if false;`). Purchases must be validated and created by server-side Cloud Functions or trusted backend webhooks.
4. **Moderation Reports (`/reports/{reportId}`)**:
   - Write-only for authenticated reporters with content length limits. Direct client reads and updates are denied.
5. **Default Deny**:
   - Any unmatched paths default to `allow read, write: if false;`.

### Cloud Storage Rules (`storage.rules`)
- Avatars and user media paths (`/users/{userId}/**`) allow writes only by the authenticated owner.
- Max file upload size is strictly capped at **5MB**.
- MIME type is constrained strictly to `image/*`.
- Default deny on all other storage paths.

---

## 6. Client Business Logic Hardening
- **Reward Manipulation Prevention**:
  - `HunterRepository.addXp(amount, reason)` validates that `amount` is within `1..10,000`. Negative, zero, or excessively high XP grants are rejected and logged to the security audit trail.
- **Shop & Balance Integrity**:
  - `buyShopItem(item)` rejects items with negative or zero currency prices.
  - Balances are verified atomically before granting items.
  - Duplicate purchases of unique items (titles, profile frames, themes) are prevented.
- **Client Rate Limiting**:
  - Critical actions (chat messaging, quest completion, purchase requests) are protected by a sliding window rate limiter in `SecurityAuditManager`.

---

## 7. Play Integrity & Device Attestation
- **Integration Points**:
  - `SecurityAuditManager.validatePlayIntegrityToken()` and `evaluateDeviceRisk()` provide client-side hooks for Play Integrity token verification.
- **Risk Assessment**:
  - Device signals (e.g. emulator flags, root artifacts) are treated as **risk advisory signals**, not as a substitute for server-side authorization.
  - The backend server must exchange the integrity token with Google Play Integrity API servers to determine app licensing and device verdict.

---

## 8. Verification & Automated Test Suite
The security features are covered by unit tests in `app/src/test/java/com/example/SecurityAuditTest.kt`:
1. `testSaltedPbkdf2PasswordHashing`: Verifies distinct salts, valid password confirmation, rejection of wrong passwords, and graceful handling of malformed hashes.
2. `testBruteForceAccountLockout`: Verifies account lockout after 5 failed attempts and subsequent rejection of login requests during the lockout window.
3. `testRateLimiterEnforcement`: Verifies burst allowance and blocking of requests exceeding capacity.
4. `testXpRewardBoundaryAndTamperProtection`: Verifies rejection of negative XP, zero XP, and stat-inflation attacks (>10,000 XP).
5. `testShopBalanceAndDuplicatePurchaseIntegrity`: Verifies rejection of purchases with insufficient balance and rejection of negative-price exploit attempts.
6. `testSensitiveLogSanitization`: Verifies redaction of bearer tokens, passwords, and sensitive credentials in audit logging.
7. `testPlayIntegrityAttestationValidation`: Verifies token validation requirements.

---

## 9. Pre-Release Deployment Checklist
Before distributing production APKs/AABs:
- [x] Cleartext HTTP disabled in `AndroidManifest.xml` and `network_security_config.xml`.
- [x] Application exclusions configured for cloud backups and device migration.
- [x] ProGuard/R8 configured to strip debug logging and protect security classes.
- [x] Firestore security rules deployed via `deploy_firebase`.
- [x] Firebase Storage security rules configured.
- [x] PBKDF2 salted password hashing implemented and verified.
- [x] Unit test suite executed and passing.
- [ ] For production releases on Google Play, configure Play Integrity API in the Google Cloud Console and link to the Play Console.
- [ ] For real-money purchases, integrate Google Play Billing with server-side Real-time Developer Notifications (RTDN).
