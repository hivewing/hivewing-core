#! /bin/bash
echo "RUNNING AWS S3 Local on port 4200"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "DIR: " $DIR
cd s3
rm -rf root/dev
mkdir -p root/dev
exec ./bin/fakes3 --port 4200 --root root/dev
