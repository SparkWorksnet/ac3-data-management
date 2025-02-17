docker buildx build --platform linux/arm/v7,linux/arm64/v8,linux/amd64 . -f rabbit/Dockerfile -t sparkworks/ac3-edge-broker:0.3 --push
