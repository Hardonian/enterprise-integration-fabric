# ADR 0004: Tenant isolation

Status: Accepted

Every tenant-owned table includes tenant_id and application endpoints scope reads by tenant. This prevents accidental cross-tenant lookup and creates a clear path to future row-level security or schema-per-tenant models.
