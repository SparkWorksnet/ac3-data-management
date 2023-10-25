# import zlib as zip
# import gzip as zip
import bz2 as zip
import os
import sys

import cv2
import numpy as np
import pika


def callback(ch, method, properties, body_compressed):
    body = zip.decompress(body_compressed)
    print(len(body))
    # get the original jpeg byte array size
    size = sys.getsizeof(body) - 33
    # jpeg-encoded byte array into numpy array
    np_array = np.frombuffer(body, dtype=np.uint8)
    np_array = np_array.reshape((size, 1))
    # decode jpeg-encoded numpy array
    image = cv2.imdecode(np_array, 1)
    # show image
    image2 = cv2.resize(image, (300, 200))
    # cv2.resizeWindow("image", len(image), len(image[0]))  # Resize window to specified dimensions
    cv2.imshow("image", image2)
    cv2.waitKey(1)

    # send message ack
    # message.ack()


def main():
    credentials = pika.PlainCredentials(os.getenv('BROKER_USERNAME'), os.getenv('BROKER_PASSWORD'))
    connection_parameters = pika.ConnectionParameters(host=os.getenv('BROKER_HOST'), port=int(os.getenv('BROKER_PORT')),
                                                      virtual_host=os.getenv('BROKER_VHOST', '/'),
                                                      credentials=credentials)
    connection = pika.BlockingConnection(connection_parameters)
    channel = connection.channel()
    queue_name = os.getenv('BROKER_QUEUE')
    channel.basic_consume(queue=queue_name, on_message_callback=callback, auto_ack=True)
    print(' [*] Waiting for messages.')
    channel.start_consuming()


if __name__ == '__main__':
    try:
        main()
        print('started main!')
    except KeyboardInterrupt:
        print('Interrupted')
        try:
            sys.exit(0)
        except SystemExit:
            os._exit(0)
