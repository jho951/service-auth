# Auth boundary for this repository

## Scope
This repository owns authentication infrastructure and host-app integration.
It does not own authorization policy.

## Do
- keep authentication focused on JWT / refresh / SSO integration
- allow authorities/roles/scopes to be carried as metadata only
- preserve the host app's manual OAuth2 + browser SSO orchestration
- prefer minimal, compile-safe changes
- run the relevant Gradle compile/test commands after edits

## Do not
- do not add permission evaluators
- do not add resource-level access decisions
- do not introduce domain authorization logic
- do not replace the manual SSO flow with the library's generic OAuth2 auto-flow
- do not add unrelated cleanup or broad refactors
