FROM python:3.10-alpine

# install python requirements
RUN pip install pika flask prometheus_client

# copy needed source files
COPY main.py ./
COPY monitoring.py ./

# execute the application's main python file
CMD python main.py -h $RABBITMQ_HOST -p $RABBITMQ_PORT -u $RABBITMQ_USERNAME -c $RABBITMQ_PASSWORD -i $QUEUE_IN
