#!/usr/bin/env bash
./build.sh

docker tag mapper:1.0 registry.sparkworks.net/mapper:1.0
docker push registry.sparkworks.net/mapper:1.0
