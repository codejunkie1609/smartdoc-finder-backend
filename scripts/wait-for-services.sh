#!/bin/sh
# wait-for-services.sh

set -e

# The host and port to check for each service
RERANKER_HOST="reranker"
RERANKER_PORT="8000"
GENERATOR_HOST="generator"
GENERATOR_PORT="8000"

# This function waits for a single service to be available
wait_for() {
  echo "Waiting for $1 to be ready..."
  # nc (netcat) is a utility to check network connections
  # The -z flag tells it to scan for listening daemons without sending any data
  while ! nc -z "$1" "$2"; do
    sleep 2
  done
  echo "$1 is ready!"
}

# Wait for each service sequentially
wait_for "$RERANKER_HOST" "$RERANKER_PORT"
wait_for "$GENERATOR_HOST" "$GENERATOR_PORT"

# Once all services are ready, execute the main command passed to the script
# (This will be the "java -jar app.jar" command from your Dockerfile)
exec "$@"
