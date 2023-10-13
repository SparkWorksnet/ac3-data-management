"""Kombu-based Video Stream Publisher

Written by Minsu Jang
Date: 2018-06-09
"""

from __future__ import absolute_import, unicode_literals

# import zlib as zip
# import gzip as zip
import bz2 as zip

import time

import cv2
import pika

# Default RabbitMQ server URI

credentials = pika.PlainCredentials('user', 'password')
connection_parameters = pika.ConnectionParameters(host='127.0.0.1',
                                                  port='5672',
                                                  virtual_host='/',
                                                  credentials=credentials)
connection = pika.BlockingConnection(connection_parameters)
channel = connection.channel()

# Video Capture by OpenCV
capture = cv2.VideoCapture(0)
encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 90]

while True:
    ret, frame = capture.read()
    if ret is True:
        # Make image smaller for faster delivery
        frame = cv2.resize(frame, None, fx=0.6, fy=0.6)
        # Encode into JPEG
        result, imgencode = cv2.imencode('.jpg', frame, encode_param)
        # # Send JPEG-encoded byte array
        compressed_data = zip.compress(imgencode.tobytes())
        print(len(imgencode.tobytes()), len(compressed_data),
              len(zip.compress(imgencode.tobytes())) / len(imgencode.tobytes()))
        channel.basic_publish(exchange='video-exchange', routing_key='hello', body=compressed_data)

    time.sleep(0.001)

capture.release()
