echo "RUNNING AWS S3 Local on port 4201"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "DIR: " $DIR
cd s3
rm -rf root/test
mkdir -p root/test
exec ./bin/fakes3 --port 4201 --root root/test
