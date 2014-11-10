{:dev
  {
   :env
    {
     :hivewing-aws-access-key "123abc",
     :hivewing-aws-secret-key "123abc",
     :hivewing-ddb-endpoint   "http://localhost:3800",
     :hivewing-ddb-worker-config-table "HivewingWorkerConfiguration.v1"
    },
   :test
    {
     :hivewing-aws-access-key "123abc",
     :hivewing-aws-secret-key "123abc",
     :hivewing-ddb-endpoint   "http://localhost:3800",
     :hivewing-ddb-worker-config-table "test.HivewingWorkerConfiguration.v1"
    }
  }
}
