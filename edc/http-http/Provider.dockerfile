# Use an official OpenJDK runtime as a parent image
#FROM openjdk:17-jdk
FROM amazoncorretto:17-alpine-jdk

# Set the working directory inside the container
WORKDIR /usr/src/app

ENV WEB_BASE_URL=http://127.0.0.1
ENV WEB_HTTP_PORT=18180
ENV WEB_HTTP_MANAGEMENT_PORT=18181
ENV WEB_HTTP_PROTOCOL_PORT=18182
ENV WEB_HTTP_CONTROL_PORT=18183

# Copy the current directory contents into the container at /usr/src/app
COPY connector.jar .

# Specify the command to run the application
CMD java -jar -Dedc.transfer.proxy.token.signer.privatekey.alias=private-key \
		-Dedc.transfer.proxy.token.verifier.publickey.alias=public-key \
		-Dweb.http.port=${WEB_HTTP_PORT} -Dweb.http.path=/api \
	    -Dweb.http.management.port=${WEB_HTTP_MANAGEMENT_PORT} -Dweb.http.management.path=/management \
	    -Dweb.http.protocol.port=${WEB_HTTP_PROTOCOL_PORT} -Dweb.http.protocol.path=/protocol \
	    -Dweb.http.control.port=${WEB_HTTP_CONTROL_PORT} -Dweb.http.control.path=/control \
	    -Dedc.dsp.callback.address=${WEB_BASE_URL}:${WEB_HTTP_PROTOCOL_PORT}/protocol \
	    -Dedc.participant.id=provider -Dedc.ids.id=urn:connector:provider \
	    -Dedc.dataplane.http.sink.partition.size=1 connector.jar


# Define the mount point for the external directory
# This will be provided at runtime when the docker container is run using the -v flag
VOLUME /data
