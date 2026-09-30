CREATE TABLE cms_module (
  code VARCHAR(32) PRIMARY KEY,
  revision BIGINT NOT NULL,
  schema_json TEXT NOT NULL
);
CREATE TABLE content_entry (
  id VARCHAR(36) PRIMARY KEY,
  module_code VARCHAR(32) NOT NULL REFERENCES cms_module(code),
  data_json TEXT NOT NULL,
  search_text TEXT NOT NULL,
  version BIGINT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_content_module_updated ON content_entry(module_code, updated_at DESC, id);
CREATE TABLE audit_event (
  id VARCHAR(36) PRIMARY KEY,
  actor VARCHAR(100) NOT NULL,
  action VARCHAR(40) NOT NULL,
  module_code VARCHAR(32) NOT NULL,
  target_id VARCHAR(36) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_audit_created ON audit_event(created_at DESC);
