ALTER TABLE workers
 ADD access_token uuid DEFAULT uuid_generate_v4();
