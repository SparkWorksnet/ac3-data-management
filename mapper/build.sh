docker buildx build --platform linux/arm/v7,linux/arm64/v8,linux/amd64 . -f Dockerfile -t sparkworks/sw-mapper-ac3:0.5 --push
