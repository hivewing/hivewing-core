ALTER TABLE hives
  ADD COLUMN apiary_uuid uuid REFERENCES apiaries (uuid) NOT NULL;
