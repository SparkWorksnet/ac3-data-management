# Run the producer

Run the `video_producer_pika.py` file.

The program opens the computer's connected camera and starts receiving image frames.
Each frame is split in bytes and sent over the message queue.

# Run the consumer

Run the `video_consumer_pika.py` file.

The program opens a connection to the rabbitmq server.
It subscribes to a queue and starts receiving frames as rabbitmq messages.
Each frame is decoded and presented in a separate window.
