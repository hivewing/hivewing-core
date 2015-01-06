DROP TABLE IF EXISTS worker_configs;

CREATE TABLE worker_configs (
  worker_uuid uuid REFERENCES workers (uuid) NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE,
  key varchar(255) NOT NULL,
  data JSON DEFAULT '{}'::JSON
);

CREATE TRIGGER set_updated_at_worker_configs
  BEFORE UPDATE ON worker_configs FOR EACH ROW
  EXECUTE PROCEDURE set_updated_at_column();

CREATE UNIQUE INDEX ON worker_configs (worker_uuid, key);
