# Contract Sync (Auth-server)

- Contract Source: https://github.com/jho951/contract
- Service SoT Branch: `main`
- Contract Role: Authentication/session/token owner

## Required Links
- Routing: https://github.com/jho951/contract/blob/main/contracts/routing.md
- Security: https://github.com/jho951/contract/blob/main/contracts/security.md
- Env: https://github.com/jho951/contract/blob/main/contracts/env.md
- User OpenAPI: https://github.com/jho951/contract/blob/main/contracts/openapi/user-service.v1.yaml

## Sync Checklist
- [ ] `USER_SERVICE_BASE_URL` uses service DNS (`http://user-service:8082`)
- [ ] internal JWT claims (`iss/aud/scope`) match contract
- [ ] user provisioning uses contract-defined endpoints
- [ ] gateway-facing auth behavior documented by contract
