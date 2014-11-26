CREATE TABLE public_keys (
  uuid uuid PRIMARY KEY DEFAULT uuid_generate_v1(),
  updated_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  beekeeper_uuid uuid REFERENCES beekeepers (uuid) NOT NULL,
  key TEXT NOT NULL
);

CREATE TRIGGER set_updated_at_public_keys
  BEFORE UPDATE ON public_keys FOR EACH ROW
  EXECUTE PROCEDURE set_updated_at_column();

CREATE UNIQUE INDEX unique_public_keys ON public_keys (key);
