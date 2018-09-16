#!/usr/bin/env bash
echo "Set local variables before start Rest API"
#echo "don't forget this command: chmod +x set_locals.sh"

export WEBSERVER_ADDRESS='0.0.0.0'
echo "WEBSERVER_ADDRESS=$WEBSERVER_ADDRESS"

export WEBSERVER_PORT=8112
echo "WEBSERVER_PORT=$WEBSERVER_PORT"

export FILESYSTEM_PATH='/home'
echo "FILESYSTEM_PATH=$FILESYSTEM_PATH"
