--  Create the hives table
CREATE TABLE hives (
  uuid uuid PRIMARY KEY DEFAULT uuid_generate_v1(),
  updated_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  name VARCHAR(255) DEFAULT 'unnamed' NOT NULL
);

CREATE TRIGGER set_updated_at_hives
  BEFORE UPDATE ON hives FOR EACH ROW
  EXECUTE PROCEDURE set_updated_at_column();

--  Create the apiaries table
CREATE TABLE apiaries (
  uuid uuid PRIMARY KEY DEFAULT uuid_generate_v1(),
  updated_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TRIGGER set_updated_at_apiaries
  BEFORE UPDATE ON apiaries FOR EACH ROW
  EXECUTE PROCEDURE set_updated_at_column();

-- Non-valid workers may be in there. Drop them. None have an apiary uuid
DELETE FROM workers;

-- Update the workers to include these things.
ALTER TABLE workers
  ADD COLUMN apiary_uuid uuid REFERENCES apiaries (uuid) NOT NULL,
  ADD COLUMN hive_uuid uuid REFERENCES hives (uuid) NOT NULL;
