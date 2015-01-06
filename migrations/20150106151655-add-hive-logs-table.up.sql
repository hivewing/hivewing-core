DROP TABLE IF EXISTS hivelogs;

CREATE TABLE hivelogs (
  at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  hive_uuid UUID REFERENCES hives (uuid) NOT NULL,
  worker_uuid UUID REFERENCES workers (uuid),
  task VARCHAR(255),
  message TEXT NOT NULL
);

CREATE INDEX ON hivelogs (at, hive_uuid);
CREATE INDEX ON hivelogs (at, hive_uuid,worker_uuid);
CREATE INDEX ON hivelogs (at, hive_uuid,task);
CREATE INDEX ON hivelogs (at, hive_uuid,worker_uuid, task);
