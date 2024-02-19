FROM rabbitmq:management-alpine
ADD rabbit/definitions.json /etc/rabbitmq/definitions.json
ADD rabbit/rabbitmq.conf /etc/rabbitmq/rabbitmq.conf
RUN rabbitmq-plugins enable --offline rabbitmq_web_stomp
RUN rabbitmq-plugins enable --offline rabbitmq_stream
