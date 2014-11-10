{:dev
  {
   :env
    {
     :hivewing-aws-access-key "123abc",
     :hivewing-aws-secret-key "123abc",
     :hivewing-ddb-endpoint   "http://localhost:3800",
     :hivewing-ddb-worker-config-table "HivewingWorkerConfiguration.v1"
     :hivewing-sql-connection-string "//localhost:5432/hivewing-dev?user=hivewing&password=hivewing"
    },
   :test
    {
     :hivewing-aws-access-key "123abc",
     :hivewing-aws-secret-key "123abc",
     :hivewing-ddb-endpoint   "http://localhost:3800",
     :hivewing-ddb-worker-config-table "test.HivewingWorkerConfiguration.v1"
     :hivewing-sql-connection-string "//localhost:5432/hivewing-test?user=hivewing&password=hivewing"
    }
  }
}
