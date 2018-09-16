#!/usr/bin/env bash
echo "Set local variables before start Worker"
#echo "don't forget this command: chmod +x set_locals.sh"

export WORKER_PORT='2771'
echo "WORKER_PORT=$WORKER_PORT"

export CASSANDRA_ADDRESS='172.17.0.3'
echo "CASSANDRA_ADDRESS=$CASSANDRA_ADDRESS"

export CASSANDRA_KEYSPACE='file_io'
echo "CASSANDRA_KEYSPACE=$CASSANDRA_KEYSPACE"

export SPARK_APP_NAME='FileIO'
echo "SPARK_APP_NAME=$SPARK_APP_NAME"

export SPARK_MASTER='local'
echo "SPARK_MASTER=$SPARK_MASTER"
