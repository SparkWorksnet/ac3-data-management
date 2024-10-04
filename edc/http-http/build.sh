# build provider
docker build -t ac3-connector-http-http-provider -f Provider.dockerfile .
docker tag ac3-connector-http-http-provider sparkworks/ac3-connector-http-http-provider:latest
docker push sparkworks/ac3-connector-http-http-provider:latest

# build consumer
docker build -t ac3-connector-http-http-consumer -f Consumer.dockerfile .
docker tag ac3-connector-http-http-consumer sparkworks/ac3-connector-http-http-consumer:latest
docker push sparkworks/ac3-connector-http-http-consumer:latest

# build http-logger
docker build -t ac3-http-request-logger -f HttpLogger.dockerfile .
docker tag ac3-http-request-logger sparkworks/ac3-http-request-logger:latest
docker push sparkworks/ac3-http-request-logger:latest
