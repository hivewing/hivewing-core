DROP TABLE IF EXISTS hivedata;

CREATE TABLE hivedata (
  at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  hive_uuid UUID REFERENCES hives (uuid) NOT NULL,
  worker_uuid UUID REFERENCES workers (uuid),
  name VARCHAR(255) NOT NULL,
  data VARCHAR(1024) NOT NULL
);

CREATE INDEX ON hivedata (hive_uuid,worker_uuid, name);
CREATE INDEX ON hivedata (at, hive_uuid);
CREATE INDEX ON hivedata (at, hive_uuid,worker_uuid);
CREATE INDEX ON hivedata (at, hive_uuid,name);
CREATE INDEX ON hivedata (at, hive_uuid,worker_uuid, name);
