ALTER TABLE workers
  DROP COLUMN apiary_uuid ,
  DROP COLUMN hive_uuid uuid ;

DROP TRIGGER set_updated_at_apiaries ON apiaries;

DROP TABLE apiaries;


DROP TRIGGER set_updated_at_hives ON hives;
DROP TABLE hives;
