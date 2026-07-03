CREATE TABLE tenants (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE institutions (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  name VARCHAR(255) NOT NULL,
  region VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_institutions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_institutions_tenant (tenant_id)
);
CREATE TABLE connectors (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  name VARCHAR(255) NOT NULL,
  system_type VARCHAR(64) NOT NULL,
  base_url VARCHAR(512) NOT NULL,
  credential_reference VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_connectors_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_connectors_tenant (tenant_id), INDEX idx_connectors_created (created_at)
);
CREATE TABLE integration_events (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  connector_id VARCHAR(64) NULL,
  event_type VARCHAR(128) NOT NULL,
  correlation_id VARCHAR(128) NOT NULL,
  payload JSON NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_events_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_events_tenant (tenant_id), INDEX idx_events_corr (correlation_id), INDEX idx_events_type (event_type), INDEX idx_events_created (created_at), INDEX idx_events_connector (connector_id)
);
CREATE TABLE event_traces (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  event_id VARCHAR(64) NULL,
  correlation_id VARCHAR(128) NOT NULL,
  route_name VARCHAR(128) NOT NULL,
  step VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  message VARCHAR(1024) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_traces_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_traces_tenant (tenant_id), INDEX idx_traces_corr (correlation_id), INDEX idx_traces_created (created_at)
);
CREATE TABLE dead_letter_events (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(128) NOT NULL,
  correlation_id VARCHAR(128) NOT NULL,
  failure_reason VARCHAR(1024) NOT NULL,
  payload JSON NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_dlq_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_dlq_tenant (tenant_id), INDEX idx_dlq_corr (correlation_id), INDEX idx_dlq_type (event_type), INDEX idx_dlq_created (created_at)
);
CREATE TABLE entitlements (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  subject_id VARCHAR(128) NOT NULL,
  product_code VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  expires_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_entitlements_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_entitlements_tenant (tenant_id), INDEX idx_entitlements_created (created_at)
);
CREATE TABLE courses (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  lms_course_id VARCHAR(128) NOT NULL,
  title VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_courses_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_courses_tenant (tenant_id), INDEX idx_courses_created (created_at)
);
CREATE TABLE enrollments (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  course_id VARCHAR(64) NOT NULL,
  learner_id VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_enrollments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_enrollments_tenant (tenant_id), INDEX idx_enrollments_created (created_at)
);
CREATE TABLE grade_events (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  course_id VARCHAR(64) NOT NULL,
  learner_id VARCHAR(128) NOT NULL,
  grade DECIMAL(5,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_grade_events_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_grade_events_tenant (tenant_id), INDEX idx_grade_events_created (created_at)
);
CREATE TABLE crm_leads (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  institution_id VARCHAR(64) NOT NULL,
  contact_email VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_crm_leads_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_crm_leads_tenant (tenant_id), INDEX idx_crm_leads_created (created_at)
);
CREATE TABLE billing_customers (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  external_customer_id VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_billing_customers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_billing_customers_tenant (tenant_id), INDEX idx_billing_customers_created (created_at)
);
