docker buildx build --platform linux/arm/v7,linux/arm64/v8,linux/amd64 . -f  ac3-reporter.dockerfile -t sparkworks/ac3-reporter:0.2 --push
