from __future__ import absolute_import, unicode_literals

# import zlib as zip
# import gzip as zip
import bz2 as zip

import time

import cv2
import pika

credentials = pika.PlainCredentials('user', 'password')
connection_parameters = pika.ConnectionParameters(host='127.0.0.1', port='5672', virtual_host='/',
                                                  credentials=credentials)
connection = pika.BlockingConnection(connection_parameters)
channel = connection.channel()

# This is the location the application will open to receive the video data
# use '0', '1' or another index for a camera connected to this device
# use the rtsp stream url for a remote feed
inputstream = '0'

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
        channel.basic_publish(exchange='video-exchange', routing_key='hello', body=compressed_data)
    time.sleep(0.0001)

capture.release()
