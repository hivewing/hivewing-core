ALTER TABLE workers
  ADD COLUMN last_seen timestamp with time zone,
  ADD COLUMN connected boolean default false;
