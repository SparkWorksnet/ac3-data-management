docker buildx build --platform linux/arm/v7,linux/arm64/v8 . -f Dockerfile -t sparkworks/ac3-edge-monitor:0.1 --push
