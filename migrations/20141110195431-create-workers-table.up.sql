DROP TABLE IF EXISTS workers;

CREATE TABLE workers (
  uuid uuid PRIMARY KEY DEFAULT uuid_generate_v1(),
  updated_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TRIGGER set_updated_at_workers
  BEFORE UPDATE ON workers FOR EACH ROW
  EXECUTE PROCEDURE set_updated_at_column();
