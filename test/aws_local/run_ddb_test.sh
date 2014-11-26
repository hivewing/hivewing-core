echo "RUNNING TEST AWS DynamoDB Local on port 3801"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "DIR: " $DIR
exec java -Djava.library.path=$DIR/ddb/DynamoDBLocal_lib -jar $DIR/ddb/DynamoDBLocal.jar -dbPath $DIR/ddb/data -port 3801
