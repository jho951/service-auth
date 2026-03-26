Read AGENTS.md first and follow it strictly.

First, inspect the repository and produce a short plan before editing any files. After the plan, implement the changes.

# Goal
Migrate this Auth-server repository from the old layered auth artifacts (`auth-contract`, `auth-spi`, `auth-common`, `auth-starter`) to the newer `jho951/auth` modular artifacts, with `auth-hybrid-spring-boot-starter` as the main starter used by this server.

# Repository context
This repository is a Spring Boot multi-module project with:
- `common/`
- `app/`
- `gradle/libs.versions.toml`

Important current files:
- `gradle/libs.versions.toml`
- `common/build.gradle`
- `app/build.gradle`
- `app/src/main/resources/dev/application-dev_auth.yml`
- `app/src/main/resources/prod/application-prod_auth.yml`
- `app/src/main/java/com/authservice/app/security/SecurityConfig.java`
- `app/src/main/java/com/authservice/app/domain/auth/controller/AuthController.java`
- `app/src/main/java/com/authservice/app/domain/auth/controller/AuthGatewayController.java`
- `app/src/main/java/com/authservice/app/domain/auth/sso/service/SsoAuthService.java`
- `app/src/main/java/com/authservice/app/domain/auth/service/AuthUserFinder.java`
- `app/src/main/java/com/authservice/app/domain/auth/service/AuthPasswordVerifier.java`
- `app/src/main/java/com/authservice/app/domain/auth/service/AuthRedisRefreshTokenStore.java`

# Architectural intent
This server will use `auth-hybrid-spring-boot-starter` as the primary auth starter.

However, because the application directly imports types from multiple auth modules, do NOT depend on only one starter artifact. Declare the direct module dependencies that the code imports.

Treat the external auth library as authentication infrastructure only. Do not add authorization or permission policy logic here.

# Required dependency target
Use version `2.0.5` of the `io.github.jho951` auth modules.

Use these coordinates:
- `io.github.jho951:auth-core`
- `io.github.jho951:auth-spring`
- `io.github.jho951:auth-jwt-spring-boot-starter`
- `io.github.jho951:auth-hybrid-spring-boot-starter`

Do NOT add:
- `auth-session-spring-boot-starter`

# Exact required edits
1. Update `gradle/libs.versions.toml`
   - change `auth-library` from `1.1.4` to `2.0.5`
   - remove these old aliases:
     - `auth-contract`
     - `auth-spi`
     - `auth-common`
     - `auth-starter`
   - define these aliases:
     - `auth-core`
     - `auth-spring`
     - `auth-jwt-starter`
     - `auth-hybrid-starter`

2. Update `common/build.gradle`
   - remove the old auth dependency alias usage
   - use `implementation libs.auth.core`
   - keep the rest of the file stable unless a compile reason requires a minimal edit

3. Update `app/build.gradle`
   - remove the old auth dependency alias usage
   - add:
     - `implementation libs.auth.core`
     - `implementation libs.auth.spring`
     - `implementation libs.auth.jwt.starter`
     - `implementation libs.auth.hybrid.starter`
   - keep `jjwt` dependencies because this application still uses `Jwts.builder()` directly in app code
   - keep other existing dependencies unless a compile reason requires a minimal edit

4. Update auth profile config files:
   - `app/src/main/resources/dev/application-dev_auth.yml`
   - `app/src/main/resources/prod/application-prod_auth.yml`

   Required config intent:
   - keep `auth.auto-security: false`
   - set `auth.oauth2.enabled: false`
   - keep refresh cookie settings intact unless a minimal compatibility edit is required
   - keep JWT secret and TTL settings intact unless a minimal compatibility edit is required
   - do not remove Spring Security OAuth2 client registration; this host app still uses Spring OAuth2 login

# Important behavioral constraints
- Keep the host app’s manual OAuth2/browser SSO flow.
- Do NOT replace or remove the existing custom SSO orchestration.
- Preserve these classes and their responsibilities:
  - `SecurityConfig`
  - `SsoOAuth2SuccessHandler`
  - `SsoOAuth2FailureHandler`
  - `SsoAuthService`
  - `SsoSessionStore`
  - `SsoCookieService`
  - `AuthUserFinder`
  - `AuthPasswordVerifier`
  - `AuthRedisRefreshTokenStore`
- Do NOT introduce permission evaluators or authorization policy logic.
- Make the smallest safe code changes needed to compile and run.
- Do not do unrelated cleanup or formatting churn.

# Why OAuth2 auto-config must be disabled in auth properties
This host application already wires its own OAuth2 success/failure handlers and browser SSO exchange flow. The library’s generic OAuth2 auto-flow must not take over.

# Validation requirements
After editing:
1. run the broadest safe Gradle verification you can
2. at minimum run:
   - `./gradlew :common:compileJava :app:compileJava`
3. if feasible, also run:
   - `./gradlew test`
4. if something cannot be run locally, explain exactly why

# Output contract
At the end, provide:
1. a concise summary of changed files
2. the exact dependency migration performed
3. the exact config changes performed
4. confirmation that manual SSO flow was preserved
5. validation commands run and their results
6. any follow-up risks or manual checks
