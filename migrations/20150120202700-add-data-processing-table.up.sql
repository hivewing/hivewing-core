DROP TABLE IF EXISTS hive_data_processing_stages;

CREATE TABLE hive_data_processing_stages (
  uuid uuid PRIMARY KEY DEFAULT uuid_generate_v1(),
  hive_uuid UUID REFERENCES hives (uuid) NOT NULL,
  stage_type VARCHAR(128) NOT NULL,
  params  json NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ON hive_data_processing_stages (hive_uuid);

CREATE TRIGGER set_updated_at_hive_data_processing_stages
  BEFORE UPDATE ON hive_data_processing_stages FOR EACH ROW
  EXECUTE PROCEDURE set_updated_at_column();
