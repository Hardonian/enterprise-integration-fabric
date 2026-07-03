Enterprise Integration Fabric is a reference architecture and implementation scaffold.

Security contact:
- Use GitHub private vulnerability reporting if enabled on the repository.
- Do not include raw connector credentials, access tokens, customer data, or tenant data in reports.

Supported security model:
- OIDC/JWT validation is enabled by default.
- Local dev-auth requires explicit DEV_AUTH_ENABLED=true.
- Connector records store credential references only.
- Tenant-owned API paths enforce tenant authorization.

Production warning:
- Do not expose this stack publicly until .env.production has strong secrets, TLS, restricted CORS, backup policy, and real OIDC tenant claims configured.
