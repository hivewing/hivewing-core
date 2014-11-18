--  Create the hives table
CREATE TABLE hive_managers (
  uuid uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  updated_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  beekeeper_uuid uuid REFERENCES beekeepers (uuid) NOT NULL,
  hive_uuid uuid REFERENCES hives (uuid) NOT NULL,
  can_write boolean DEFAULT false
);

CREATE TRIGGER set_updated_at_hive_managers
  BEFORE UPDATE ON hive_managers FOR EACH ROW
  EXECUTE PROCEDURE set_updated_at_column();
