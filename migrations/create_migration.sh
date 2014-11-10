#! /bin/bash
export TS=`date -u +%Y%m%d%H%M%S`
touch "migrations/$TS-$1.up.sql"
touch "migrations/$TS-$1.down.sql"
