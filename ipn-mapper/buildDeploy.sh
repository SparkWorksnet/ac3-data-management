#!/usr/bin/env bash
./build.sh

docker tag ipn-mapper:1.0 registry.sparkworks.net/ipn-mapper:1.0
docker push registry.sparkworks.net/ipn-mapper:1.0
