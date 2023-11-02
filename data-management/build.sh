docker buildx build --platform linux/arm/v7,linux/arm64/v8 . -f rabbit/Dockerfile -t sparkworks/ac3-edge-broker:0.1 --push
