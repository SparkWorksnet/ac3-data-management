#!/usr/bin/env bash
docker build --build-arg JAR_FILE=./target/mapper-1.0-SNAPSHOT.jar -t mapper:1.0 .

