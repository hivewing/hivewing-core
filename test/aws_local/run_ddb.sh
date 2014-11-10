echo "RUNNING AWS DynamoDB Local on port 3800"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -Djava.library.path=$DIR/ddb/DynamoDBLocal_lib -jar $DIR/ddb/DynamoDBLocal.jar -dbPath $DIR/ddb/data -port 3800
