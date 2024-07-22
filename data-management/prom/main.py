import getopt
import logging
import sys

import pika
from prometheus_client import Gauge

from monitoring import start_prometheus_monitoring

LOGGING_FORMAT = '%(asctime)s - %(filename)s:%(lineno)d - %(levelname)s - %(message)s'
# initialize logger
logger = logging.getLogger('main')
logging.basicConfig(level=logging.INFO, format=LOGGING_FORMAT)

gauges = {}

devices = []

if __name__ == '__main__':
    argv = sys.argv[1:]

    rabbit_host = None
    rabbit_port = None
    rabbit_username = None
    rabbit_password = None
    topic_in = None

    try:
        opts, args = getopt.getopt(argv, 'vh:p:u:c:i:',
                                   ['host=', 'port=', 'username=', 'password=', 'topicin='])
    except getopt.GetoptError:
        logging.info('main.py -h <host> -p <port> -u <username> -c <password> -i <topicin>')
        sys.exit(2)
    for opt, arg in opts:
        if opt in ('-h', '--host'):
            rabbit_host = arg
        elif opt in ('-p', '--port'):
            rabbit_port = arg
        elif opt in ('-u', '--username'):
            rabbit_username = arg
        elif opt in ('-c', '--password'):
            rabbit_password = arg
        elif opt in ('-i', '--topicin'):
            topic_in = arg

    # start monitoring interface
    start_prometheus_monitoring(port=5004)

    credentials = pika.PlainCredentials(rabbit_username, rabbit_password)
    connection_parameters = pika.ConnectionParameters(host=rabbit_host,
                                                      port=rabbit_port,
                                                      virtual_host='/',
                                                      credentials=credentials)
    connection = pika.BlockingConnection(connection_parameters)

    channel = connection.channel()


    def handle_message(rk, b):
        logger.info(rk)
        parts = str(b).split(',')
        value = parts[1]
        device = parts[0].split('/')[2]
        sensor = parts[0].split('/')[3]
        if sensor not in gauges:
            gauges[sensor] = Gauge(
                f"current_{sensor}_label",
                "docum",
                ["device"],
            )
        try:
            gauges[sensor].labels(device).set(float(value))
            devices.append(device)
        except:
            pass


    def on_message(chan, method_frame, header_frame, body, userdata=None):
        handle_message(method_frame.routing_key, body)


    channel.basic_consume(topic_in, on_message, auto_ack=True)

    channel.start_consuming()
