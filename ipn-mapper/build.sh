#!/usr/bin/env bash
docker build --build-arg JAR_FILE=./target/ipn-mapper-1.0-SNAPSHOT.jar -t ipn-mapper:1.0 .

