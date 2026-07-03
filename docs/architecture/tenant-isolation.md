# Tenant isolation

Application isolation is enforced by tenant-scoped route parameters and tenant_id validation during event ingestion. Database isolation is enforced by tenant_id columns, foreign keys to tenants, and tenant_id indexes on all tenant-owned tables. Cross-tenant connector fetches return 404 instead of leaking existence details.
