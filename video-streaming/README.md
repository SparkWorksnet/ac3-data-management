# Run the broker

Run the broker using the following command: `docker compose -f docker-compose.yml up -d`

This stack starts a RabbitMQ broker and opens access to it through port 5672. The default username/password
is `guest`/`guest`.

# Run the producer

Run the `video_producer_pika.py` file.

The program opens a video file and sends the video frames to the broker.
Each frame is split in bytes and sent over the message queue.

To send the computer's camera change the `inputstream` to `0`.
To receive the video from an RTSP stream change the `inputstream` to the rtsp url.

# Run the consumer

Run the `video_consumer_pika.py` file.

The program opens a connection to the rabbitmq server.
It subscribes to a queue and starts receiving frames as rabbitmq messages.
Each frame is decoded and presented in a separate window.
