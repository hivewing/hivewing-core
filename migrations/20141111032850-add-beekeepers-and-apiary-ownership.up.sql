CREATE TABLE beekeepers (
  uuid uuid PRIMARY KEY DEFAULT uuid_generate_v1(),
  updated_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  email VARCHAR(255) NOT NULL
);

CREATE TRIGGER set_updated_at_beekeepers
  BEFORE UPDATE ON beekeepers FOR EACH ROW
  EXECUTE PROCEDURE set_updated_at_column();

-- Now add references from the apiairy to these.
ALTER TABLE apiaries
  ADD COLUMN beekeeper_uuid uuid REFERENCES beekeepers (uuid) NOT NULL;
