from __future__ import absolute_import, unicode_literals

# import zlib as zip
# import gzip as zip
import bz2 as zip
import os

import time

import cv2
import pika

credentials = pika.PlainCredentials(os.getenv('BROKER_USERNAME', "guest"), os.getenv('BROKER_PASSWORD', "guest"))
connection_parameters = pika.ConnectionParameters(host=os.getenv('BROKER_HOST', "127.0.0.1"),
                                                  port=os.getenv('BROKER_PORT', "5672"),
                                                  virtual_host=os.getenv('BROKER_VHOST', '/'),
                                                  credentials=credentials)
connection = pika.BlockingConnection(connection_parameters)
channel = connection.channel()

exchange_name = os.getenv('BROKER_EXCHANGE', "amq.topic")
routing_key = os.getenv('BROKER_ROUTING_KEY', exchange_name)

queue_name = os.getenv('BROKER_QUEUE', "output")

channel.queue_declare(queue_name, passive=False, durable=True)
channel.queue_bind(queue_name, exchange_name, "#")

inputstream = os.getenv('INPUT_STREAM', "test.mp4")

print(f"connecting to {inputstream}")

# Video Capture by OpenCV
capture = cv2.VideoCapture(inputstream if not inputstream.isnumeric() else int(inputstream))

while capture is not None:
    encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 90]
    ret, frame = capture.read()
    if ret is True:
        frame = cv2.resize(frame, None, fx=0.6, fy=0.6)
        result, imgencode = cv2.imencode('.jpg', frame, encode_param)
        compressed_data = zip.compress(imgencode.tobytes())
        print(len(imgencode.tobytes()), len(compressed_data),
              len(zip.compress(imgencode.tobytes())) / len(imgencode.tobytes()))
        channel.basic_publish(exchange=exchange_name, routing_key=routing_key, body=compressed_data)
    time.sleep(0.0001)

capture.release()
