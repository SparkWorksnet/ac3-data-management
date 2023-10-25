import os
import pika
import json

# RabbitMQ setup
credentials = pika.PlainCredentials(os.getenv('BROKER_USERNAME'), os.getenv('BROKER_PASSWORD'))
connection_parameters = pika.ConnectionParameters(host=os.getenv('BROKER_HOST'), port=int(os.getenv('BROKER_PORT')),
                                                  virtual_host=os.getenv('BROKER_VHOST', '/'),
                                                  credentials=credentials)
connection = pika.BlockingConnection(connection_parameters)
channel = connection.channel()
exchange_name = os.getenv('BROKER_EXCHANGE')

# Message
message_data = {
    'source_bucket': 'ac3-filestore-1',
    'source_file': 'final_cube_LR-V.fits',
    'destination_bucket': 'ac3-filestore-2',
    'destination_file': 'test/cube-1.fits'
}
message_body = json.dumps(message_data)

routing_key = exchange_name
channel.basic_publish(exchange=exchange_name, routing_key=routing_key, body=message_body)

print(" [x] Message data sent: " + message_body)

connection.close()
